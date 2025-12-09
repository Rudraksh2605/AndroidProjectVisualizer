package com.projectvisualizer.parsers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.projectvisualizer.model.CodeComponent;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UniversalNavigationParser {

    public void extractNavigation(File file, CodeComponent component) {
        if (file.getName().endsWith(".java")) {
            parseJavaNavigation(file, component);
        } else if (file.getName().endsWith(".kt")) {
            parseKotlinNavigation(file, component);
        }
    }

    private void parseJavaNavigation(File file, CodeComponent component) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(file);

            cu.findAll(ObjectCreationExpr.class).forEach(obj -> {
                if (obj.getType().asString().equals("Intent")) {
                    obj.getArguments().forEach(arg -> {
                        String argStr = arg.toString();
                        if (argStr.endsWith(".class")) {
                            String target = argStr.replace(".class", "");
                            if (target.contains(".")) target = target.substring(target.lastIndexOf(".") + 1);
                            component.addNavigationTarget(target);
                        }
                    });
                }
            });

            cu.findAll(MethodCallExpr.class).forEach(method -> {
                String name = method.getNameAsString();
                if (name.equals("replace") || name.equals("add")) {
                    method.getArguments().forEach(arg -> {
                        if (arg.isObjectCreationExpr()) {
                            String type = arg.asObjectCreationExpr().getType().asString();
                            component.addNavigationTarget(type);
                        }
                    });
                }
            });

        } catch (Exception e) {
            System.err.println("Error parsing Java navigation: " + e.getMessage());
        }
    }

    private void parseKotlinNavigation(File file, CodeComponent component) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()));

            Pattern intentPattern = Pattern.compile("Intent\\s*\\([^,]+,\\s*([A-Za-z0-9_]+)::class\\.java\\)");
            Matcher intentMatcher = intentPattern.matcher(content);
            while (intentMatcher.find()) {
                component.addNavigationTarget(intentMatcher.group(1));
            }

            Pattern ktxPattern = Pattern.compile("startActivity\\s*<\\s*([A-Za-z0-9_]+)\\s*>");
            Matcher ktxMatcher = ktxPattern.matcher(content);
            while (ktxMatcher.find()) {
                component.addNavigationTarget(ktxMatcher.group(1));
            }

            Pattern fragmentPattern = Pattern.compile("\\.(replace|add)\\s*\\([^,]+,\\s*([A-Za-z0-9_]+)\\(\\)\\)");
            Matcher fragmentMatcher = fragmentPattern.matcher(content);
            while (fragmentMatcher.find()) {
                component.addNavigationTarget(fragmentMatcher.group(2));
            }

            Pattern navPattern = Pattern.compile("navigate\\s*\\(\\s*R\\.id\\.([A-Za-z0-9_]+)");
            Matcher navMatcher = navPattern.matcher(content);
            while (navMatcher.find()) {
                component.addNavigationTarget("action:" + navMatcher.group(1));
            }

        } catch (Exception e) {
            System.err.println("Error parsing Kotlin navigation: " + e.getMessage());
        }
    }
}