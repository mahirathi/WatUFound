# WatuFound System Design Document

## 1. Executive Summary

WatuFound is an Android-based lost and found application designed for the University of Waterloo community. The system enables users to post lost items, report found items, communicate with other users, and manage their profiles. The application follows a client-server architecture with Firebase as the backend service provider.

## 2. System Architecture Overview

### 2.1 High-Level Architecture

The WatuFound system follows a **Model-View-ViewModel (MVVM)** architecture pattern with the following components:

- **Client Layer**: Android application with UI components
- **Business Logic Layer**: ViewModels and utility classes
- **Data Layer**: Firebase Firestore and Realtime Database
- **Authentication Layer**: Firebase Authentication

### 2.2 Technology Stack

- **Frontend**: Android (Kotlin) with XML layouts
- **Backend**: Firebase Firestore (NoSQL database)
- **Real-time Communication**: Firebase Realtime Database
- **Authentication**: Firebase Authentication
- **Image Storage**: Firebase Storage
- **Maps Integration**: OpenStreetMap (OSMDroid)

## 3. Class Design and Structure

### 3.1 Core Domain Models

#### User Model
```kotlin
data class User(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: UserRole = UserRole.USER,
    val isActive: Boolean = true,
    val isBanned: Boolean = false,
    val banReason: String = "",
    val bannedAt: Long = 0,
    val bannedBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)
```

#### Item Models
```kotlin
data class FoundItem(
    val documentId: String = "",
    val itemTitle: String = "",
    val description: String = "",
    val category: String = "",
    val foundAt: String = "",
    val roomNumber: String = "",
    val dateTimeFound: String = "",
    val dateCreated: String = "",
    val userId: String = "",
    val userName: String = "",
    val isAnonymous: Boolean = false,
    val imageUrl: String? = null,
    val isResolved: Boolean = false,
    val isDeleted: Boolean = false
)

data class LostItem(
    val documentId: String = "",
    val itemTitle: String = "",
    val description: String = "",
    val category: String = "",
    val lastSeenAt: String = "",
    val roomNumber: String = "",
    val dateTimeLost: String = "",
    val dateCreated: String = "",
    val userId: String = "",
    val userName: String = "",
    val isAnonymous: Boolean = false,
    val imageUrl: String? = null,
    val isResolved: Boolean = false,
    val isDeleted: Boolean = false
)
```

#### Message Model
```kotlin
data class Message(
    val id: String = "",
    val text: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val timestamp: Long = 0
)
```

### 3.2 ViewModels (Business Logic Layer)

#### HomeViewModel
- **Purpose**: Manages the main feed functionality
- **Key Responsibilities**:
  - Fetch and filter posts (lost/found items)
  - Handle search and filtering operations
  - Manage post sorting and display
- **Design Pattern**: MVVM with Observer pattern

#### AdminViewModel
- **Purpose**: Manages administrative functions
- **Key Responsibilities**:
  - User management and statistics
  - System monitoring
  - Admin role verification
- **Design Pattern**: MVVM with Singleton pattern for admin access

#### ConversationsViewModel
- **Purpose**: Manages messaging functionality
- **Key Responsibilities**:
  - User search and conversation management
  - Real-time message handling
  - Conversation state management
- **Design Pattern**: MVVM with Observer pattern

### 3.3 Adapters (UI Components)

#### PostAdapter
- **Purpose**: Displays posts in RecyclerView
- **Key Features**:
  - Handles both lost and found items
  - Image loading with Glide
  - Navigation to post details
- **Design Pattern**: Adapter pattern

#### UserAdapter
- **Purpose**: Displays user lists for admin
- **Key Features**:
  - User status display (active/banned)
  - Profile identification
  - Click handling
- **Design Pattern**: Adapter pattern

#### MessagesAdapter
- **Purpose**: Displays messages in conversations
- **Key Features**:
  - Different layouts for sent/received messages
  - Timestamp formatting
  - Message state management
- **Design Pattern**: Adapter pattern with DiffUtil

### 3.4 Utility Classes

#### CategoryUtils
- **Purpose**: Centralized category management
- **Key Features**:
  - Predefined categories
  - Image restriction rules
  - Category validation

#### TimeUtils
- **Purpose**: Time formatting and calculations
- **Key Features**:
  - Relative time display
  - Date parsing
  - Timestamp formatting

#### BuildingLocationUtils
- **Purpose**: Campus building management
- **Key Features**:
  - Building coordinate mapping
  - CSV data loading
  - Location validation

## 4. Design Patterns Implementation

### 4.1 Model-View-ViewModel (MVVM) Pattern

**Implementation**: Throughout the application
- **Models**: User, FoundItem, LostItem, Message
- **Views**: Fragments and Activities
- **ViewModels**: HomeViewModel, AdminViewModel, ConversationsViewModel

**Rationale**: 
- Separation of concerns between UI and business logic
- Testability and maintainability
- Lifecycle awareness for Android components
- Reactive programming with LiveData

**Alternative Considered**: MVP (Model-View-Presenter)
- **Rejection Reason**: MVVM provides better integration with Android Architecture Components and automatic lifecycle management.

### 4.2 Observer Pattern

**Implementation**: LiveData in ViewModels
```kotlin
private val _allPosts = MutableLiveData<List<FeedItem>>()
val allPosts: LiveData<List<FeedItem>> = _allPosts
```

**Rationale**:
- Automatic lifecycle management
- Prevents memory leaks
- Reactive UI updates
- Thread-safe data handling

**Alternative Considered**: RxJava
- **Rejection Reason**: LiveData is more Android-specific and requires less boilerplate code.

### 4.3 Adapter Pattern

**Implementation**: RecyclerView adapters
- PostAdapter for feed items
- UserAdapter for user lists
- MessagesAdapter for conversations

**Rationale**:
- Standard Android pattern for list display
- Efficient view recycling
- Separation of data and presentation logic

### 4.4 Singleton Pattern

**Implementation**: Utility classes and Firebase instances
```kotlin
object CategoryUtils {
    val ALL_CATEGORIES = listOf(...)
}
```

**Rationale**:
- Centralized configuration management
- Memory efficiency
- Global access to utilities

## 5. Class Diagram

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   MainActivity  │    │  LoginActivity  │    │ SignUpActivity  │
│                 │    │                 │    │                 │
│ - binding       │    │ - auth          │    │ - auth          │
│ - auth          │    │ - email         │    │ - email         │
│ - navController │    │ - password      │    │ - password      │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│  HomeFragment   │    │ FoundFormFrag   │    │  LostFormFrag   │
│                 │    │                 │    │                 │
│ - viewModel     │    │ - viewModel     │    │ - viewModel     │
│ - adapter       │    │ - imagePicker   │    │ - imagePicker   │
│ - recyclerView  │    │ - locationPicker│    │ - locationPicker│
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ HomeViewModel   │    │FoundFormViewModel│    │ LostFormViewModel│
│                 │    │                 │    │                 │
│ - _allPosts     │    │ - _formData     │    │ - _formData     │
│ - _filteredPosts│    │ - _isLoading    │    │ - _isLoading    │
│ - filterData    │    │ - _imageUrl     │    │ - _imageUrl     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PostAdapter   │    │  FoundItem      │    │   LostItem      │
│                 │    │                 │    │                 │
│ - posts         │    │ - documentId    │    │ - documentId    │
│ - onItemClick   │    │ - itemTitle     │    │ - itemTitle     │
│ - updatePosts() │    │ - description   │    │ - description   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   FeedItem      │    │     User        │    │    Message      │
│                 │    │                 │    │                 │
│ - Lost()        │    │ - uid           │    │ - id            │
│ - Found()       │    │ - displayName   │    │ - text          │
└─────────────────┘    │ - email         │    │ - senderId      │
                       │ - role          │    │ - senderName    │
                       │ - isActive      │    │ - timestamp     │
                       │ - isBanned      │    └─────────────────┘
                       └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │  UserRole       │
                       │                 │
                       │ - USER          │
                       │ - MODERATOR     │
                       │ - ADMIN         │
                       └─────────────────┘
```

## 6. Sequence Diagrams

### 6.1 User Authentication Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   User      │    │MainActivity │    │LoginActivity│    │Firebase Auth│
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │                   │
       │ 1. Launch App     │                   │                   │
       │─────────────────>│                   │                   │
       │                   │                   │                   │
       │                   │ 2. Check Auth    │                   │
       │                   │─────────────────────────────────────>│
       │                   │                   │                   │
       │                   │ 3. No User       │                   │
       │                   │<─────────────────────────────────────│
       │                   │                   │                   │
       │                   │ 4. Start Login   │                   │
       │                   │─────────────────>│                   │
       │                   │                   │                   │
       │ 5. Enter Credentials                 │                   │
       │─────────────────────────────────────>│                   │
       │                   │                   │                   │
       │                   │ 6. Authenticate  │                   │
       │                   │─────────────────────────────────────>│
       │                   │                   │                   │
       │                   │ 7. Success       │                   │
       │                   │<─────────────────────────────────────│
       │                   │                   │                   │
       │                   │ 8. Initialize App│                   │
       │                   │─────────────────>│                   │
       │                   │                   │                   │
       │ 9. Show Main UI   │                   │                   │
       │<─────────────────│                   │                   │
```

### 6.2 Post Creation Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   User      │    │FoundFormFrag│    │FoundFormVM  │    │Firebase     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │                   │
       │ 1. Fill Form      │                   │                   │
       │─────────────────>│                   │                   │
       │                   │                   │                   │
       │ 2. Select Image   │                   │                   │
       │─────────────────>│                   │                   │
       │                   │                   │                   │
       │ 3. Upload Image   │                   │                   │
       │                   │─────────────────────────────────────>│
       │                   │                   │                   │
       │ 4. Image URL      │                   │                   │
       │                   │<─────────────────────────────────────│
       │                   │                   │                   │
       │ 5. Submit Form    │                   │                   │
       │─────────────────>│                   │                   │
       │                   │ 6. Validate Data  │                   │
       │                   │─────────────────>│                   │
       │                   │                   │                   │
       │                   │ 7. Save to DB    │                   │
       │                   │─────────────────────────────────────>│
       │                   │                   │                   │
       │                   │ 8. Success       │                   │
       │                   │<─────────────────────────────────────│
       │                   │                   │                   │
       │ 9. Navigate Back  │                   │                   │
       │<─────────────────│                   │                   │
```

### 6.3 Messaging Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   User A    │    │ConversationF│    │ConversationVM│   │Firebase DB  │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │                   │
       │ 1. Send Message   │                   │                   │
       │─────────────────>│                   │                   │
       │                   │                   │                   │
       │                   │ 2. Create Message│                   │
       │                   │─────────────────>│                   │
       │                   │                   │                   │
       │                   │ 3. Save to DB    │                   │
       │                   │─────────────────────────────────────>│
       │                   │                   │                   │
       │                   │ 4. Success       │                   │
       │                   │<─────────────────────────────────────│
       │                   │                   │                   │
       │ 5. Update UI      │                   │                   │
       │<─────────────────│                   │                   │
       │                   │                   │                   │
       │                   │ 6. Real-time Update                  │
       │                   │<─────────────────────────────────────│
       │                   │                   │                   │
       │ 7. Show Message   │                   │                   │
       │<─────────────────│                   │                   │
```

## 7. Data Flow and Storage

### 7.1 Firebase Collections Structure

```
Firebase Firestore:
├── users/
│   ├── {userId}/
│   │   ├── displayName
│   │   ├── email
│   │   ├── role
│   │   ├── isActive
│   │   └── isBanned
├── lost_items/
│   ├── {itemId}/
│   │   ├── itemTitle
│   │   ├── description
│   │   ├── category
│   │   ├── lastSeenAt
│   │   ├── userId
│   │   └── isResolved
└── found_items/
    ├── {itemId}/
    │   ├── itemTitle
    │   ├── description
    │   ├── category
    │   ├── foundAt
    │   ├── userId
    │   └── isResolved

Firebase Realtime Database:
├── conversations/
│   ├── {conversationId}/
│   │   ├── {messageId}/
│   │   │   ├── text
│   │   │   ├── senderId
│   │   │   └── timestamp
└── userProfiles/
    ├── {userId}/
    │   ├── displayName
    │   └── email
```

### 7.2 Data Flow Patterns

1. **Read Operations**: ViewModels fetch data from Firebase and expose through LiveData
2. **Write Operations**: User actions trigger ViewModel methods that update Firebase
3. **Real-time Updates**: Firebase listeners automatically update UI when data changes
4. **Caching**: ViewModels maintain local state for immediate UI updates

## 8. Security and Authentication

### 8.1 Authentication Strategy

- **Firebase Authentication**: Email/password authentication
- **Role-based Access**: User, Moderator, Admin roles
- **Admin Verification**: Email domain-based admin detection

### 8.2 Data Security

- **Firestore Rules**: Server-side validation
- **User Isolation**: Users can only access their own data
- **Admin Override**: Admins have elevated permissions

## 9. Performance Considerations

### 9.1 Optimization Strategies

1. **Pagination**: Load posts in batches
2. **Image Caching**: Glide library for efficient image loading
3. **RecyclerView**: Efficient list rendering
4. **DiffUtil**: Smart list updates

### 9.2 Memory Management

1. **ViewModel Lifecycle**: Automatic cleanup
2. **LiveData**: Lifecycle-aware data holders
3. **Image Loading**: Memory-efficient image handling

## 10. Testing Strategy

### 10.1 Unit Testing

- **ViewModel Tests**: Business logic validation
- **Utility Tests**: Helper function verification
- **Model Tests**: Data structure validation

### 10.2 Integration Testing

- **Firebase Integration**: Database operation testing
- **UI Testing**: User interaction validation
- **Navigation Testing**: Screen flow verification

## 11. Deployment and Distribution

### 11.1 Build Configuration

- **Gradle**: Dependency management
- **ProGuard**: Code obfuscation
- **Firebase**: Backend service configuration

### 11.2 Distribution

- **Google Play Store**: Primary distribution channel
- **APK Distribution**: Alternative distribution method

## 12. Future Enhancements

### 12.1 Planned Features

1. **Push Notifications**: Real-time alerts
2. **Advanced Search**: AI-powered item matching
3. **Analytics**: User behavior tracking
4. **Offline Support**: Local data caching

### 12.2 Scalability Considerations

1. **Database Optimization**: Indexing and query optimization
2. **CDN Integration**: Global content delivery
3. **Microservices**: Backend service decomposition

## 13. Conclusion

The WatuFound system design follows modern Android development best practices with a focus on maintainability, scalability, and user experience. The MVVM architecture provides clear separation of concerns, while Firebase services offer reliable backend functionality. The implementation of design patterns ensures code reusability and testability, making it suitable for junior programmers to understand and extend.

The system's modular design allows for easy feature additions and modifications, while the comprehensive documentation provides clear guidance for implementation and integration. 