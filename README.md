# Integrated Inventory Management - CS 499 Capstone

**[View the Full ePortfolio & Documentation Here](https://mtndewboi.github.io)**

This repository contains the enhanced version of an Android Inventory Management application, originally developed for CS 360 and significantly improved for the CS 499 Computer Science Capstone. The project showcases a comprehensive refactoring process, transforming a basic prototype into a professional-grade application.

## Project Evolution: CS 360 vs. CS 499

The capstone project involved a critical evaluation and enhancement of the original application, focusing on modern software architecture, algorithmic efficiency, data integrity, and security.

| Feature Area | Original (CS 360) | Enhanced (CS 499) |
| :--- | :--- | :--- |
| **Architecture** | Monolithic "God Activity" | **MVVM (Model-View-ViewModel)** |
| **UI/UX** | Fixed, non-responsive layout | Responsive tablet layouts, dynamic theming |
| **Data Handling** | Direct database calls | Repository pattern, LiveData updates |
| **Database** | Single flat table, no history | **Relational schema** with `usage_log` for history |
| **Algorithms** | Basic linear search | In-memory sorting, custom graphing, predictive analysis |
| **Security** | Loose input validation | Strict validation, security-first mindset |

## Core Enhancements

### 1. Software Design & Architecture
The application was refactored from a tightly-coupled, monolithic structure to the **Model-View-ViewModel (MVVM)** pattern. This separation of concerns improves maintainability, testability, and ensures the app handles configuration changes (like screen rotation) without data loss.

*   **Responsive UI:** Adapts gracefully to both phone and tablet screens.
*   **Dynamic Theming:** Centralized theme management allows for runtime color scheme switching.

### 2. Algorithms & Data Structures
Custom algorithms were implemented to provide valuable insights from the inventory data, moving beyond simple list display.

*   **Efficient Sorting:** In-memory sorting provides instant UI feedback.
*   **Data Visualization:** A custom graphing engine visualizes usage history without external libraries.
*   **Predictive Analytics:** A "burn rate" calculator forecasts when items will run out of stock.

### 3. Database & Data Integrity
The database schema was upgraded from a simple flat table to a relational model to support advanced features.

*   **Historical Tracking:** A new `usage_log` table tracks every inventory change, enabling audit trails and analytics.
*   **Data Integrity:** Strict validation rules prevent data corruption and ensure consistency.

### 4. Security Mindset
A security-first approach was integrated throughout the enhancement process, focusing on robust input validation and future-proofing the architecture against potential vulnerabilities.

## Technology Stack
-   **Language:** Java
-   **Platform:** Android
-   **Architecture:** MVVM (Model-View-ViewModel), Repository Pattern
-   **Database:** SQLite
