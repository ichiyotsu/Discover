// swift-tools-version:5.9
import PackageDescription
let package = Package(name: "TokiDiscoverCore", platforms: [.iOS(.v16)],
    products: [.library(name: "TokiDiscoverCore", targets: ["TokiDiscoverCore"])],
    targets: [.target(name: "TokiDiscoverCore"), .testTarget(name: "TokiDiscoverCoreTests", dependencies: ["TokiDiscoverCore"])])
