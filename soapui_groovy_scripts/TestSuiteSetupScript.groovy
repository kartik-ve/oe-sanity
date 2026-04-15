// Environment Configuration & Initialization

def envConfig = [
    "8285": [
        hostname: "illnqw8285",
        endpoint: "http://illnqw8285:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8374:1521:CHCDB8285"
    ],
    "8289": [
        hostname: "illnqw8289",
        endpoint: "http://illnqw8289:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8377:1521:CHCDB8289"
    ],
    "8342": [
        hostname: "illnqw8342",
        endpoint: "http://illnqw8342:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8393:1521:CHCDB8342"
    ],
    "8365": [
        hostname: "illnqw8365",
        endpoint: "http://illnqw8365:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8404:1521:CHCDB8365"
    ],
    "8645": [
        hostname: "illnqw8645",
        endpoint: "http://illnqw8645:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8655:1521:CHCDB8645"
    ],
    "8665": [
        hostname: "illnqw8665",
        endpoint: "http://illnqw8665:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8696:1521:CHCDB8665"
    ],
    "8666": [
        hostname: "illnqw8666",
        endpoint: "http://illnqw8666:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8695:1521:CHCDB8666"
    ],
    "8667": [
        hostname: "illnqw8667",
        endpoint: "http://illnqw8667:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8697:1521:CHCDB8667"
    ],
    "8731": [
        hostname: "illnqw8731",
        endpoint: "http://illnqw8731:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8740:1521:CHCDB8731"
    ],
    "8733": [
        hostname: "illnqw8733",
        endpoint: "http://illnqw8733:11111",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@illnqw8692:1521:CHCDB8733"
    ],
    "SIT1": [
        hostname: "mwhlvchca01",
        endpoint: "https://sit1-oe-npe.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@mwhlvchcadb01:1521:CHCDB1"
    ],
    "QA1": [
        hostname: "mwhlvchca02",
        endpoint: "https://qa1-oe-npe.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@mwhlvchcadb02:1521:CHCDB2"
    ],
    "UAT1": [
        hostname: "mwhlvchca03",
        endpoint: "https://uat1-oe-npe.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@mwhlvchcadb03:1521:CHCDB3"
    ],
    "HF1": [
        hostname: "mwhlvchca04",
        endpoint: "https://hf1-oe-npe.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:OMS1OMS/OMS1OMS@mwhlvchcadb04:1521:CHCDB4"
    ],
    "PLAB": [
        hostname: "mwhlvchcaamc101",
        endpoint: "https://plab-oe-omni.amdocs.spectrum.com",
        jdbc: "jdbc:oracle:thin:CHCOMS/chc_4c0nn@chcplscn1:1521/chcomspl1"
    ]
]

def project = context.testSuite.project
def activeEnv = project.activeEnvironment
def env = ""

if (activeEnv && activeEnv.name != "Default environment" && activeEnv.name != "Default") {
    log.info "ENV found: ${activeEnv.name}. Continuing..."

    def service = activeEnv.getRestServiceAt(0) ?: activeEnv.getSoapServiceAt(0)
    def endpoint = service?.getEndpoint()?.getEndpointString() ?: ""

    if (endpoint) {
        def matcher = endpoint =~ /illnqw(\d+)/
        if (matcher.find()) {
            env = matcher[0][1]
        }
    }
} else {
    env = System.getProperty("env") ?: project.getPropertyValue("ENV")

    if (!env) {
        log.error "ENV not provided! Stopping TestSuite execution..."
        throw new RuntimeException("ENV not provided")
    }

    env = env.trim().toUpperCase()
}

def config = envConfig[env]

if (!config) {
    throw new RuntimeException("Invalid ENV: " + env)
}

def endpoint = config.endpoint
def jdbcUrl = config.jdbc
def username = "omswrk1"
def hostname = config.hostname

project.setPropertyValue("ENV", env)
project.setPropertyValue("MecEndpoint", endpoint)
project.setPropertyValue("MecDBConnection", jdbcUrl)
project.setPropertyValue("USER", username)
project.setPropertyValue("HOST", hostname)

log.info "ENV: ${env}"
log.info "Endpoint: ${endpoint}"
log.info "JDBC URL: ${jdbcUrl}"
log.info "User: ${username}"
log.info "Host: ${hostname}"
