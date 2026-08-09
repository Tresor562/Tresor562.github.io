import SwiftUI
import WebKit

struct ContentView: View {
    var body: some View {
        StoreWebView(url: URL(string: "https://tresor562.github.io/nexus-store/")!)
            .background(Color(red: 7/255, green: 11/255, blue: 20/255))
    }
}

struct StoreWebView: UIViewRepresentable {
    let url: URL

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.defaultWebpagePreferences.allowsContentJavaScript = true
        let webView = WKWebView(frame: .zero, configuration: config)
        context.coordinator.webView = webView
        context.coordinator.startObserving()
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.contentInsetAdjustmentBehavior = .automatic
        webView.isOpaque = false
        webView.backgroundColor = UIColor(red: 7/255, green: 11/255, blue: 20/255, alpha: 1)
        webView.load(URLRequest(url: url, cachePolicy: .reloadRevalidatingCacheData))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}

    static func dismantleUIView(_ uiView: WKWebView, coordinator: Coordinator) {
        coordinator.stopObserving()
    }

    final class Coordinator: NSObject {
        weak var webView: WKWebView?
        private var openURLObserver: NSObjectProtocol?

        func startObserving() {
            guard openURLObserver == nil else { return }
            openURLObserver = NotificationCenter.default.addObserver(
                forName: .nexusOpenURL,
                object: nil,
                queue: .main
            ) { [weak self] notification in
                guard let url = notification.object as? URL else { return }
                self?.webView?.load(URLRequest(url: url, cachePolicy: .reloadRevalidatingCacheData))
            }
        }

        func stopObserving() {
            if let openURLObserver {
                NotificationCenter.default.removeObserver(openURLObserver)
                self.openURLObserver = nil
            }
        }

        deinit {
            stopObserving()
        }
    }
}
