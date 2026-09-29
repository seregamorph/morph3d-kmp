import SwiftUI
import UIKit
import Shared

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
                .statusBarHidden()
                .persistentSystemOverlays(.hidden)
        }
        .onChange(of: scenePhase, initial: true) { _, phase in
            // it's a screen saver, don't let the display dim and lock. The flag is ignored when set
            // before the app is active and is reset by the system when the app leaves the foreground,
            // so it's (re)applied on each activation. Toggling forces UIKit to re-evaluate it.
            guard phase == .active else { return }
            UIApplication.shared.isIdleTimerDisabled = false
            UIApplication.shared.isIdleTimerDisabled = true
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
