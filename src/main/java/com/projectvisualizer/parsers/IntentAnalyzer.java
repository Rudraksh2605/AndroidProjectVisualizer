package com.projectvisualizer.parsers;

import com.projectvisualizer.model.*;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.io.File;
import java.util.*;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IntentAnalyzer {

    public List<NavigationFlow> analyzeIntentFlows(File javaFile) throws Exception {
        List<NavigationFlow> flows = new ArrayList<>();

        if (javaFile.getName().endsWith(".kt")) {
            String content = new String(Files.readAllBytes(javaFile.toPath()));
            flows.addAll(analyzeKotlinNavigation(content, javaFile));
            return flows;
        }

        String content = new String(Files.readAllBytes(javaFile.toPath()));

        try {
            CompilationUnit cu = StaticJavaParser.parse(content);
            cu.accept(new IntentVisitor(flows, javaFile), null);
        } catch (Exception e) {
            System.err.println("Error parsing Java file for intents: " + e.getMessage());
        }

        return flows;
    }

    private static class IntentVisitor extends VoidVisitorAdapter<Void> {
        private final List<NavigationFlow> flows;
        private final File sourceFile;
        private final Map<String, List<MethodCallExpr>> methodCalls = new HashMap<>();

        public IntentVisitor(List<NavigationFlow> flows, File sourceFile) {
            this.flows = flows;
            this.sourceFile = sourceFile;
        }

        @Override
        public void visit(MethodDeclaration n, Void arg) {
            // Collect all method calls in this method
            List<MethodCallExpr> calls = new ArrayList<>();
            n.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MethodCallExpr call, Void arg) {
                    calls.add(call);
                    super.visit(call, arg);
                }
            }, null);
            methodCalls.put(n.getNameAsString(), calls);
            super.visit(n, arg);
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            String methodName = n.getNameAsString();

            // Check for startActivity calls
            if (methodName.equals("startActivity") || methodName.equals("startActivityForResult")) {
                NavigationFlow flow = extractNavigationFlow(n);
                if (flow != null) {
                    flows.add(flow);
                }
            }

            // Check for Intent creation
            if (n.getScope().isPresent() && n.getScope().get().toString().equals("Intent")) {
                // This might be Intent constructor call
                analyzeIntentCreation(n);
            }

            super.visit(n, arg);
        }

        @Override
        public void visit(ObjectCreationExpr n, Void arg) {
            // Check for Intent object creation
            if (n.getType().asString().equals("Intent")) {
                NavigationFlow flow = extractNavigationFlowFromIntent(n);
                if (flow != null) {
                    flows.add(flow);
                }
            }
            super.visit(n, arg);
        }

        private NavigationFlow extractNavigationFlow(MethodCallExpr startActivityCall) {
            // Analyze the startActivity call to extract navigation information
            NavigationFlow flow = new NavigationFlow();
            flow.setFlowId(UUID.randomUUID().toString());
            flow.setSourceScreenId(extractClassName(sourceFile.getName()));
            flow.setNavigationType(NavigationFlow.NavigationType.FORWARD);

            // The argument to startActivity should be an Intent
            if (!startActivityCall.getArguments().isEmpty()) {
                String intentExpr = startActivityCall.getArgument(0).toString();
                flow.setTargetScreenId(extractTargetFromIntent(intentExpr));

                // Analyze conditions around this call
                extractNavigationConditions(startActivityCall, flow);
            }

            return flow;
        }

        private NavigationFlow extractNavigationFlowFromIntent(ObjectCreationExpr intentCreation) {
            NavigationFlow flow = new NavigationFlow();
            flow.setFlowId(UUID.randomUUID().toString());
            flow.setSourceScreenId(extractClassName(sourceFile.getName()));
            flow.setNavigationType(NavigationFlow.NavigationType.FORWARD);

            // Extract target from Intent constructor parameters
            if (!intentCreation.getArguments().isEmpty()) {
                String target = intentCreation.getArgument(0).toString();
                flow.setTargetScreenId(extractClassNameFromTarget(target));
            }

            return flow;
        }

        private void analyzeIntentCreation(MethodCallExpr intentCall) {
            // Analyze Intent method calls like putExtra, setClass, etc.
        }

        private void extractNavigationConditions(MethodCallExpr startActivityCall, NavigationFlow flow) {
            // Analyze the surrounding code for conditions
            startActivityCall.getParentNode().ifPresent(parent -> {
                parent.findCompilationUnit().ifPresent(cu -> {
                    cu.accept(new VoidVisitorAdapter<Void>() {
                        @Override
                        public void visit(com.github.javaparser.ast.stmt.IfStmt n, Void arg) {
                            // Check if this if statement contains our startActivity call
                            if (n.findAll(MethodCallExpr.class).contains(startActivityCall)) {
                                String condition = n.getCondition().toString();
                                NavigationFlow.NavigationCondition navCondition =
                                        new NavigationFlow.NavigationCondition("CONDITIONAL", condition, true);
                                flow.addCondition(navCondition);
                            }
                            super.visit(n, arg);
                        }
                    }, null);
                });
            });

            // Look for putExtra calls on the same Intent
            startActivityCall.getParentNode().ifPresent(parent -> {
                parent.findAll(MethodCallExpr.class).forEach(call -> {
                    if (call.getNameAsString().equals("putExtra")) {
                        String key = call.getArgument(0).toString();
                        String value = call.getArgument(1).toString();
                        NavigationFlow.NavigationCondition condition =
                                new NavigationFlow.NavigationCondition("DATA_EXTRA", key + "=" + value, false);
                        flow.addCondition(condition);
                    }
                });
            });
        }


        private String extractClassName(String fileName) {
            return fileName.replace(".java", "").replace(".kt", "");
        }

        private String simpleNameFromFqn(String fqn) {
            if (fqn == null) return "";
            int idx = fqn.lastIndexOf('.');
            return idx >= 0 ? fqn.substring(idx + 1) : fqn;
        }

        private String extractTargetFromIntent(String intentExpression) {
            // Parse intent expression to extract target class
            if (intentExpression.contains(".class")) {
                return intentExpression.replace(".class", "").substring(intentExpression.lastIndexOf('.') + 1);
            } else if (intentExpression.contains("\"")) {
                String className = intentExpression.replace("\"", "");
                return className.substring(className.lastIndexOf('.') + 1);
            }
            return "Unknown";
        }

        private String extractClassNameFromTarget(String target) {
            if (target.contains(".class")) {
                return target.replace(".class", "").substring(target.lastIndexOf('.') + 1);
            } else if (target.contains("\"")) {
                String className = target.replace("\"", "");
                return className.substring(className.lastIndexOf('.') + 1);
            }
            return target;
        }
    }

    // Helper in outer class
    private String simpleNameFromFileName(String fileName) {
        if (fileName == null) return "";
        int idx = fileName.lastIndexOf('.');
        return idx > 0 ? fileName.substring(0, idx) : fileName;
    }

    // Lightweight Kotlin navigation analysis using regex scanning
    private List<NavigationFlow> analyzeKotlinNavigation(String content, File sourceFile) {
        List<NavigationFlow> flows = new ArrayList<>();
        if (content == null) return flows;
        String src = sourceFile != null ? sourceFile.getName() : "unknown.kt";
        String source = simpleNameFromFileName(src);

        try {
            Pattern intentCtor = Pattern.compile("Intent\\s*\\(\\s*[^,]*,\\s*([A-Za-z0-9_\\.]+)::class\\.java\\s*\\)");
            Matcher m1 = intentCtor.matcher(content);
            while (m1.find()) {
                String target = m1.group(1);
                String simple = target.substring(target.lastIndexOf('.') + 1);
                NavigationFlow flow = new NavigationFlow();
                flow.setFlowId(UUID.randomUUID().toString());
                flow.setSourceScreenId(source);
                flow.setTargetScreenId(simple);
                flow.setNavigationType(NavigationFlow.NavigationType.FORWARD);
                flows.add(flow);
            }

            Pattern startActivity = Pattern.compile("startActivity\\s*\\(.*?([A-Za-z0-9_\\.]+)::class\\.java.*?\\)", Pattern.DOTALL);
            Matcher m2 = startActivity.matcher(content);
            while (m2.find()) {
                String target = m2.group(1);
                String simple = target.substring(target.lastIndexOf('.') + 1);
                NavigationFlow flow = new NavigationFlow();
                flow.setFlowId(UUID.randomUUID().toString());
                flow.setSourceScreenId(source);
                flow.setTargetScreenId(simple);
                flow.setNavigationType(NavigationFlow.NavigationType.FORWARD);
                flows.add(flow);
            }

            Pattern navNavigate = Pattern.compile("\\.navigate\\s*\\(\\s*(R\\.id\\.[A-Za-z0-9_]+|\"[^\"]+\")");
            Matcher m3 = navNavigate.matcher(content);
            while (m3.find()) {
                String destToken = m3.group(1);
                String dest = destToken.replace("\"", "");
                NavigationFlow flow = new NavigationFlow();
                flow.setFlowId(UUID.randomUUID().toString());
                flow.setSourceScreenId(source);
                flow.setTargetScreenId(dest);
                flow.setNavigationType(NavigationFlow.NavigationType.FORWARD);
                flows.add(flow);
            }

            Pattern fragTxn = Pattern.compile("\\.(replace|add)\\s*\\(.*?,\\s*([A-Za-z0-9_\\.]+)\\s*\\(\\s*\\)\\s*\\)", Pattern.DOTALL);
            Matcher m4 = fragTxn.matcher(content);
            while (m4.find()) {
                String fragmentClass = m4.group(2);
                String simple = fragmentClass.substring(fragmentClass.lastIndexOf('.') + 1);
                NavigationFlow flow = new NavigationFlow();
                flow.setFlowId(UUID.randomUUID().toString());
                flow.setSourceScreenId(source);
                flow.setTargetScreenId(simple);
                flow.setNavigationType(NavigationFlow.NavigationType.FORWARD);
                flows.add(flow);
            }
        } catch (Exception ignored) { }

        return flows;
    }
}