rootProject.name = "spring-cqrs-axon-poc"

// services
include("auth")
include("gateway")
include("messaging")
include("otp")

// shared libraries
include("security-core")
include("spring-core")
include("spring-metrics")
include("spring-web")
include("spring-axon-reactor")

// apis
include("api-core")
include("api-messaging")
include("api-otp")