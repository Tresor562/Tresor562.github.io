import SwiftUI
import WebKit

struct ContentView: View {
    var body: some View {
        StoreWebView(url: URL(string: "https://tresor562.github.io/nexus-store/")!)
            .ignoresSafeArea(edges: .bottom)
    }
}

struct StoreWebView: UIViewRepresentable {
    let url: URL

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.defaultWebpagePreferences.allowsContentJavaScript = true
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.allowsBackForwardNavigationGestures = true
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}
}
