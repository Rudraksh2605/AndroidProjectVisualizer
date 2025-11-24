package com.projectvisualizer.services;

import com.projectvisualizer.model.CodeComponent;
import java.util.*;
import java.util.regex.Pattern;

public class ComponentCategorizer {
    private static final Map<String, List<Pattern>> CATEGORY_PATTERNS = new HashMap<>();

    static {
        // UI Components - Comprehensive patterns
        CATEGORY_PATTERNS.put("UI", Arrays.asList(
                Pattern.compile(".*(activity|fragment|adapter|viewholder|view|layout|dialog|menu|button|text|image|list|recycler|card).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(screen|page|composable|widget|component|uicomponent|uiwidget|customview).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(textview|edittext|imageview|recyclerview|listview|cardview|constraintlayout).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(linearlayout|relativelayout|framelayout|scrollview|viewpager|tablayout).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(navigationview|drawerlayout|coordinatorlayout|appbarlayout|floatingactionbutton).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(snackbar|bottomnavigationview|toolbar|actionbar|progressbar|seekbar).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(switch|checkbox|radiobutton|spinner|webview|mapview|surfaceview|textureview).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(calendarview|datepicker|timepicker|numberpicker|ratingbar|searchview|videoview).*", Pattern.CASE_INSENSITIVE)
        ));

        // API Calling and Network Components
        CATEGORY_PATTERNS.put("BUSINESS_LOGIC", Arrays.asList(
                Pattern.compile(".*(api|volley|retrofit|okhttp|gson|moshi|converter|call|request|response).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(network|http|https|rest|webservice|endpoint|client|apiclient|service).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(apirepository|socket|websocket|rpc|grpc|soap|graphql|fetch|axios|ajax).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(xmlhttprequest|resttemplate|webclient|feign|restassured|mockwebserver|wiremock).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(stub|mock|fake|testdouble).*", Pattern.CASE_INSENSITIVE)
        ));

        // Enhanced Authentication and Security Components
        CATEGORY_PATTERNS.put("BUSINESS_LOGIC", Arrays.asList(
                Pattern.compile(".*(auth|authentication|authenticator|authenticate).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(signin|signup|signout|login|logout|logon|logoff|register|registration|registrar).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(user|username|password|credential|jwt|token|oauth|oauth2|openid|openidconnect).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(sso|singlesignon|firebaseauth|firebaseauthentication|biometric|fingerprint|faceid|touchid).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(security|encryption|decryption|crypto|cipher|keystore|certificate|ssl|tls|secure).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(permission|authorization|acl|role|policy|guard|middleware|interceptor).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(securityconfig|securityfilter|securitycontext|principle|identity|verification|validate|validation).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(twofactor|2fa|mfa|multifactor|otp|onetimepassword|smsauth|emailauth|emailverification).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(phoneauth|phoneverification|socialauth|sociallogin|googleauth|facebookauth|twitterauth).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(githubauth|linkedinauth|microsoftauth|appleauth|passkey|webauthn|fido|fido2).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(passwordless|magiclink|passwordreset|forgotpassword|recoveraccount|accountrecovery).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(session|sessionmanagement|cookie|csrf|xsrf|xss|cors|csrfprotection|xssprotection).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(clickjacking|sqlinjection|injection|hashing|hash|salt|pepper|bcrypt|scrypt|argon2|pbkdf2).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(hmac|rsa|aes|des|3des|blowfish|ellipticcurve|ecdsa|ed25519|rs256|hs256|es256|ps256).*", Pattern.CASE_INSENSITIVE)
        ));

        // Enhanced Database and Storage Components
        CATEGORY_PATTERNS.put("BUSINESS_LOGIC", Arrays.asList(
                Pattern.compile(".*(database|db|sql|room|firebase|firestore|realm|sharedpreferences|preference).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(storage|file|cache|dao|entity|table|column|query|cursor|transaction|migration).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(schema|index|constraint|relationship|orm|objectbox|greendao|sqlite|mysql|postgresql|postgres).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(mongodb|couchbase|couchdb|watermelon|nosql|document|collection|bucket|blob|objectstorage).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(dynamodb|cassandra|redis|memcached|hbase|hive|pig|hadoop|spark|elasticsearch|solr).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(s3|gcs|azureblob|cosmosdb|cosmos|documentdb|graphdb|neo4j|arangodb|orientdb|dgraph|janusgraph).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(timeseries|influxdb|prometheus|graphite|clickhouse|bigquery|bigtable|datastore).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(firebasedatabase|realtimedatabase|cloudfirestore|cloudstorage|filestore|diskstore).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(persistence|persistent|repository|repo|datasource|datamanager|dataaccess|datalayer).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(crud|create|read|update|delete|insert|select|upsert|merge|truncate|drop|alter).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(index|view|trigger|procedure|function|storedprocedure|sequence|foreignkey|primarykey).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(unique|check|default|auto_increment|serial|uuid|guid|migration|versioning|rollback).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(commit|savepoint|lock|deadlock|isolation|transaction|acid|base|cap|brewers|theorem).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(sharding|partition|replication|cluster|node|master|slave|primary|secondary|replica).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(backup|restore|dump|load|import|export|etl|extract|transform|load|warehouse).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(datalake|datamart|dataset|dataframe|rdd|resilient|distributed|dataset|django).*", Pattern.CASE_INSENSITIVE)
        ));

        // Backend/Server Technologies
        CATEGORY_PATTERNS.put("BUSINESS_LOGIC", Arrays.asList(
                Pattern.compile(".*(spring|springboot|springframework|springmvc|springdata|springsecurity).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(flask|django|fastapi|node|express|nestjs|server|backend|controller|endpoint).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(route|middleware|filter|interceptor|handler|resolver|service|microservice|gateway).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(loadbalancer|proxy|reverse|deployment|container|docker|kubernetes|k8s).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(aws|azure|gcp|cloud|lambda|function|serverless|vercel|netlify|heroku).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(digitalocean|linode|vps|vm|virtualmachine|ec2|s3|apigateway|apimanagement).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(elasticbeanstalk|appservice|functions|cloudfunctions|cloudrun|appengine|computeengine).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(firebasefunctions|supabase).*", Pattern.CASE_INSENSITIVE)
        ));

        // Data Model Components
        CATEGORY_PATTERNS.put("DATA_MODEL", Arrays.asList(
                Pattern.compile(".*(entity|model|pojo|dto|vo|bean|data|table|schema|column|field|property).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(user|product|item|order|cart|payment|account|profile|settings|config|preference).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(configuration|attribute|valueobject|domainobject|businessobject|record|struct).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(classobject|dataclass|datatransferobject|viewobject|valueobject|entitybean).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(domainmodel|datamodel|objectmodel|classmodel|uml|entityrelationship|er|erd).*", Pattern.CASE_INSENSITIVE)
        ));

        // Business Logic Components
        CATEGORY_PATTERNS.put("BUSINESS_LOGIC", Arrays.asList(
                Pattern.compile(".*(viewmodel|presenter|usecase|service|manager|handler|repository|datasource|interactor).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(facade|strategy|command|delegate|mediator|proxy|adapter|bridge|composite|decorator).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(flyweight|observer|publisher|subscriber|event|listener|visitor|template|state|memento).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(iterator|chain|responsibility|interpreter|specification|rule|engine|workflow|pipeline).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(orchestrator|saga|transaction|compensating|business|logic|processor|executor).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(coordinator|scheduler|timer|job|task|worker|thread|async|parallel|concurrent).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(sync|synchronization|mutex|semaphore|lock|atomic|transactional).*", Pattern.CASE_INSENSITIVE)
        ));

        // Navigation Components
        CATEGORY_PATTERNS.put("NAVIGATION", Arrays.asList(
                Pattern.compile(".*(intent|navigate|navigation|launch|open|start|goto|gotofragment|gotoactivity).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(action|destination|route|flow|transition|fragmenttransaction|navcontroller).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(pendingintent|activityresult|deeplink|universallink|applink|deeplinking|routing).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(router|routeconfig|routemap|routeguard|navigationdrawer|bottomnavigation).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(tabnavigation|stacknavigation|drawernavigation|bottomtabnavigator|stacknavigator).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(drawernavigator|switch|redirect|forward|back|pop|push|replace|add|remove).*", Pattern.CASE_INSENSITIVE),
                Pattern.compile(".*(show|hide|display|present|dismiss).*", Pattern.CASE_INSENSITIVE)
        ));
    }

    public static String detectCategory(CodeComponent component) {
        if (component == null || component.getName() == null) {
            return "UNKNOWN";
        }

        String name = component.getName().toLowerCase();
        String type = component.getType() != null ? component.getType().toLowerCase() : "";

        // Check each category in priority order
        for (Map.Entry<String, List<Pattern>> entry : CATEGORY_PATTERNS.entrySet()) {
            String category = entry.getKey();
            for (Pattern pattern : entry.getValue()) {
                if (pattern.matcher(name).matches() || pattern.matcher(type).matches()) {
                    return category;
                }
            }
        }

        return "UNKNOWN";
    }

    public static Map<String, String> getCategoryDisplayNames() {
        Map<String, String> displayNames = new HashMap<>();
        displayNames.put("UI", "UI Components");
        displayNames.put("DATA_MODEL", "Data Models");
        displayNames.put("BUSINESS_LOGIC", "Business Logic");
        displayNames.put("NAVIGATION", "Navigation");
        displayNames.put("UNKNOWN", "Unknown");
        return displayNames;
    }

    public static String getDisplayName(String category) {
        return getCategoryDisplayNames().getOrDefault(category, category);
    }

    // Additional utility methods for enhanced functionality
    public static Map<String, Integer> analyzeComponentDistribution(List<CodeComponent> components) {
        Map<String, Integer> distribution = new HashMap<>();
        for (CodeComponent component : components) {
            String category = detectCategory(component);
            distribution.put(category, distribution.getOrDefault(category, 0) + 1);
        }
        return distribution;
    }

    public static List<String> getCategories() {
        return Arrays.asList("UI", "DATA_MODEL", "BUSINESS_LOGIC", "NAVIGATION", "UNKNOWN");
    }

    public static boolean isInCategory(CodeComponent component, String category) {
        return detectCategory(component).equals(category);
    }
}