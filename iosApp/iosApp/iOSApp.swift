import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        InitKoinKt.doInitKoin(configuration: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
