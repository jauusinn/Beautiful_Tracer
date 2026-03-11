# Beautiful Tracer 🌐

**Beautiful Tracer** is a powerful and elegant Android network utility designed to perform deep traceroute analysis and real-time connectivity monitoring. Built with modern Android technologies (Jetpack Compose, Room, KSP, and Coroutines), it provides a seamless and visually rich experience for network debugging and discovery.

Version: **0.1 beta**

---

## ✨ Features

### 🚀 Advanced Traceroute
- **Persistent Discovery**: Unlike standard tools, Beautiful Tracer doesn't give up. It traces up to **128 hops** and continues until it explicitly reaches the destination or is stopped by the user.
- **Timeout Handling**: Clearly identifies non-responsive nodes with a custom `X----X----X` marker instead of confusing blanks.
- **Smart DNS Resolution**: Robustly resolves hostnames and URLs (e.g., `google.com`) to IPv4 addresses on background threads to ensure UI responsiveness.

### 📊 Real-time Monitoring
- **Continuous Pinging**: Once a hop is discovered, the app starts an independent, background ping loop to monitor latency and packet loss in real-time.
- **Visual Feedback**: Real-time progress bars for packet loss and color-coded latency badges (Excellent to Timeout).

### 🔍 Deep IP Insights (WHOIS)
- **Geolocation**: Discover where your data is traveling. Integrated with IP-API to provide country codes and geolocation data.
- **Country Flags**: Automatically enriches hops with country flags and icons (e.g., 🏠 for private networks).
- **Detailed Bottom Sheet**: Click any responsive node to view full WHOIS details, including ISP, organization, and location.

### 🎨 Beautiful & Adaptive UI
- **Jetpack Compose**: A fully reactive UI that auto-scrolls and animates as new hops are discovered.
- **Destination Highlighting**: Reached your target? The destination node is highlighted with a unique theme (**#FFB3BA** background with **#B22234** typography) and displays your original search term for clarity.
- **Search History**: Integrated Room database stores your recent searches. Access them instantly via a dropdown menu when you click the search bar.

---

## 🛠 Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Concurrency**: Kotlin Coroutines & Flow
- **Local Database**: Room (with KSP) for Search History & WHOIS caching
- **Networking**: Retrofit & Moshi for API communication
- **Architecture**: MVVM (Model-View-ViewModel)

---

## 🏗 Installation & Setup

### Requirements
- Android 7.0 (API 24) or higher.
- Internet connection.

### Building from Source
1. Clone the repository.
2. Open the project in **Android Studio Ladybug** (or newer).
3. Sync Gradle (Uses Kotlin 2.1.0 and KSP 2.1.0-1.0.29).
4. Build and run the `:app` module.

### Install provided APK

---

## 🔒 Permissions
- `android.permission.INTERNET`: Required to perform network traces and fetch WHOIS data.
- `android.permission.ACCESS_NETWORK_STATE`: Used to verify connectivity status.

---

## 📝 Developer Notes
This app is currently in **Beta (0.1)**. It uses a Network Security Configuration to safely allow cleartext communication with the `ip-api.com` free tier. 

Developed with ❤️ by **Jauusinn**.
