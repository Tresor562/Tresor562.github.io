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

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.defaultWebpagePreferences.allowsContentJavaScript = true
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.allowsBackForwardNavigationGestures = true
        webView.scrollView.contentInsetAdjustmentBehavior = .automatic
        webView.isOpaque = false
        webView.backgroundColor = UIColor(red: 7/255, green: 11/255, blue: 20/255, alpha: 1)
        webView.load(URLRequest(url: url, cachePolicy: .reloadRevalidatingCacheData))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}
}
