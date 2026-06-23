# Smart Wardrobe 👕👗

Smart Wardrobe is an interactive Android application designed to help users digitize their wardrobe, manage their clothing items, track Cost-Per-Wear (CPW) analytics, and receive AI/algorithmic outfit recommendations from a personal stylist assistant.

---

## 🚀 Features

### 1. Digital Wardrobe Management (`WardrobeFragment`)
* **Add Clothes**: Catalog clothing items with photos, category (T-shirt, pants, shoes, jacket), fabric type, color hex code, and purchase price.
* **Smart Images**: Supports transparent `.png` backgrounds for clean outfit layering.
* **Interactive List**: View all your clothes in a clean grid and filter items by category dynamically.

### 2. Personal Stylist (`StylistFragment`)
* **Outfit Recommendations**: Generates outfit suggestions and combinations based on matching algorithms.
* **Weather & Context Pairing**: Matches clothes to create cohesive look combinations depending on the styling criteria.

### 3. Analytics Dashboard (`AnalyticsFragment`)
* **Key Metrics**: Instantly view **Total Items**, **Total Wardrobe Portfolio Value** (in RM/$), and **Average Cost-Per-Wear (CPW)**.
* **Category Distribution**: View a proportional progress bar visualization showing how your clothes are distributed across categories (Teal for shirts, Purple for pants, Red for shoes, Orange for jackets).
* **Cost-Per-Wear Optimization**: View your **Most Worn** and **Least Worn** items to see which clothes are giving you the best return on investment.

---

## 🛠️ Tech Stack & Libraries

* **Core**: Java (Android SDK)
* **Architecture**: MVVM (Model-View-ViewModel) with Android Lifecycle Components (`ViewModel`, `LiveData`)
* **Database**: Room Database (SQLite backend) for persistence of clothing items and wear history
* **UI & Layouts**: View Binding, XML layouts with Google Material Design components, dynamic GridLayouts
* **Image Loading**: Glide library for smooth image retrieval and caching
* **Utilities**: Custom helper scripts for SQLite database testing/migrations in `/scratch`

---

## 📸 Screenshots

| Wardrobe Dashboard | Stylist Recommendations | Analytics Dashboard | Profile / Settings |
|:---:|:---:|:---:|:---:|
| ![Wardrobe](./screen_wardrobe_current.png) | ![Stylist](./screen_dark_stylist.png) | ![Analytics](./screen_dark_analytics.png) | ![Profile](./screen_dark_profile.png) |

---

## 📥 How to Run the Project

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Uinaa0/YOUR-REPOSITORY-NAME.git
   ```
2. **Open in Android Studio**:
   * Select **File > Open** and choose the `AimanNewFyp` root directory.
   * Allow Gradle build to sync and download dependencies.
3. **Run the App**:
   * Build the project and deploy it on an Android Emulator or a physical device (API level 26+ recommended).
