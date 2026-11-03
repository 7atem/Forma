// swift-tools-version: 5.10
import PackageDescription
let package = Package(name: "FormaCore", platforms: [.iOS(.v17), .watchOS(.v10), .macOS(.v13)],
    products: [.library(name: "FormaCore", targets: ["FormaCore"])],
    targets: [.target(name: "FormaCore"), .testTarget(name: "FormaCoreTests", dependencies: ["FormaCore"], resources: [.copy("Fixtures")])])
