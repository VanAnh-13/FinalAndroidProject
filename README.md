# HealthyLife Hub - Smart Health Management App

A comprehensive Android health tracking application built with MVVM architecture, Firebase, and Material Design 3. Track your health metrics, manage medications, set smart reminders, and maintain your medical records all in one place.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Project Structure](#project-structure)
- [Implementation Guide](#implementation-guide)
- [Base Classes Usage](#base-classes-usage)
- [Examples](#examples)
- [Best Practices](#best-practices)
- [Contributing](#contributing)

## 🎯 Overview

HealthyLife Hub is a modern health management application that helps users:

- **Track Health Metrics**: Monitor blood pressure, blood sugar, weight, BMI, and more
- **Smart Reminders**: AI-powered medication and appointment reminders
- **Medical Records**: Store and manage medical history, test results, and prescriptions
- **OCR Medicine Scanning**: Add medications by scanning prescription labels
- **Data Analytics**: Visual charts and insights into your health trends
- **Offline Support**: Full offline functionality with cloud sync

### ✨ Current Features (Implemented)

- ✅ **Welcome/Onboarding Screen** - 3 swipeable slides introducing the app
- ✅ **Login System** - Email/Password + Google Sign-In with Firebase
- ✅ **First-Time Detection** - Smart onboarding flow
- ✅ **Remember Login** - Persistent user sessions
- ✅ **Material Design 3** - Modern, beautiful UI

**Progress**: 2/25 screens (8%) | Phase 1 - Authentication Flow

## 🏗️ Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   View Layer    │    │  ViewModel      │    │  Repository     │
│                 │    │                 │    │                 │
│ • Activities    │◄──►│ • BaseViewModel │◄──►│ • BaseRepository│
│ • Fragments     │    │ • LiveData      │    │ • DataState     │
│ • Adapters      │    │ • Loading State │    │ • API Service   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### Core Components

- **BaseActivity**: Foundation for all activities with view binding
- **BaseFragment**: Foundation for all fragments with ViewModel integration
- **BaseAdapter**: Generic RecyclerView adapter with view binding
- **BaseViewModel**: ViewModel with loading state and async task management
- **BaseRepository**: Repository with async operations and error handling
- **DataState**: Result pattern for success/error handling

## 📋 Prerequisites

- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: 11 or higher
- **Android SDK**: API level 24 (Android 7.0) or higher
- **Gradle**: 8.13 or compatible version

## 🚀 Quick Start

### 1. Clone the Project

```bash
git clone https://github.com/VanAnh-13/FinalAndroidProject.git
cd Base
```

### 2. Setup Firebase (REQUIRED - 15 minutes)

**📖 Detailed Guide**: See [FIREBASE_SETUP.md](FIREBASE_SETUP.md)

Quick steps:
1. Create Firebase project at [console.firebase.google.com](https://console.firebase.google.com/)
2. Add Android app: `com.example.healthylifehub`
3. Download `google-services.json` → place in `app/` folder
4. Enable Authentication (Email/Password + Google)
5. Enable Firestore Database
6. Get Web Client ID → Update `AuthRepository.java` line 42

### 3. Build & Run

```bash
# Sync Gradle
./gradlew build

# Install on device/emulator
./gradlew installDebug

# Or in Android Studio: Run → Run 'app' (Shift+F10)
```

### 4. Test the App

1. ✅ Launch → See Welcome screen with 3 slides
2. ✅ Swipe between slides
3. ✅ Click "Đăng nhập" → Login screen
4. ✅ Test email validation
5. ✅ Login with test account or Google

**📚 Full Guide**: See [QUICK_START.md](QUICK_START.md)

## 📁 Project Structure

```
Base/
├── app/
│   ├── src/main/java/com/example/base/
│   │   ├── MainActivity.java                 # Main application entry
│   │   ├── base/                            # Foundation classes
│   │   │   ├── BaseActivity.java            # Base activity class
│   │   │   ├── BaseAdapter.java             # Base RecyclerView adapter
│   │   │   ├── BaseFragment.java            # Base fragment class
│   │   │   ├── BaseRepository.java          # Base repository class
│   │   │   ├── BaseViewHolder.java          # Base ViewHolder class
│   │   │   ├── BaseViewModel.java           # Base ViewModel class
│   │   │   └── DataState.java               # Result pattern implementation
│   │   ├── data/source/network/             # Network layer
│   │   │   ├── APIService.java              # Retrofit API interface
│   │   │   └── RetrofitClient.java          # HTTP client configuration
│   │   └── utils/constant/                  # Constants and configuration
│   │       └── APIConstant.java             # API constants
│   └── src/main/res/                        # Resources (layouts, strings, etc.)
├── build.gradle.kts                         # App-level build configuration
├── settings.gradle.kts                      # Project settings
└── README.md                               # This file
```

## 🛠️ Implementation Guide

### Creating a New Activity

```java
public class UserActivity extends BaseActivity<ActivityUserBinding> {
    
    public UserActivity() {
        super(ActivityUserBinding::inflate);
    }

    @Override
    public void initData() {
        // Initialize variables, setup initial state
        setupToolbar();
        loadUserData();
    }

    @Override
    public void bindData() {
        // Bind data to views, setup adapters
        userAdapter = new UserAdapter(UserItemBinding::inflate);
        getBinding().recyclerView.setAdapter(userAdapter);
    }

    @Override
    public void setOnClick() {
        // Setup click listeners
        getBinding().btnSave.setOnClickListener(v -> saveUser());
        getBinding().btnCancel.setOnClickListener(v -> finish());
    }
}
```

### Creating a New Fragment

```java
public class ProfileFragment extends BaseFragment<FragmentProfileBinding> {
    
    private ProfileViewModel viewModel;

    public ProfileFragment() {
        super(FragmentProfileBinding::inflate);
    }

    @Override
    protected BaseViewModel getViewModel() {
        if (viewModel == null) {
            viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        }
        return viewModel;
    }

    @Override
    public void initData() {
        // Initialize fragment data
        viewModel.loadProfile();
    }

    @Override
    public void bindData() {
        // Bind initial data to views
        updateUI();
    }

    @Override
    public void observeData() {
        // Observe ViewModel LiveData
        viewModel.getProfile().observe(getViewLifecycleOwner(), profile -> {
            if (profile != null) {
                displayProfile(profile);
            }
        });
    }

    @Override
    public void setOnClick() {
        // Setup click listeners
        getBinding().btnEdit.setOnClickListener(v -> editProfile());
    }
}
```

### Creating a Custom Adapter

```java
public class UserAdapter extends BaseAdapter<User, ItemUserBinding> {

    public UserAdapter() {
        super(ItemUserBinding::inflate);
    }

    @Override
    public void bindData(ItemUserBinding binding, User user, int position) {
        // Bind user data to views
        binding.textName.setText(user.getName());
        binding.textEmail.setText(user.getEmail());
        
        // Load user avatar
        Glide.with(binding.getRoot().getContext())
             .load(user.getAvatarUrl())
             .into(binding.imageAvatar);
    }

    @Override
    public void onItemClick(ItemUserBinding binding, User user, int position) {
        // Handle item click
        binding.getRoot().setOnClickListener(v -> {
            // Navigate to user detail or perform action
            Intent intent = new Intent(v.getContext(), UserDetailActivity.class);
            intent.putExtra("user_id", user.getId());
            v.getContext().startActivity(intent);
        });
    }
}
```

### Creating a ViewModel

```java
public class UserViewModel extends BaseViewModel {
    
    private final UserRepository repository;
    private final MutableLiveData<List<User>> users = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();

    public UserViewModel() {
        this.repository = new UserRepository();
    }

    public LiveData<List<User>> getUsers() {
        return users;
    }

    public LiveData<String> getError() {
        return error;
    }

    public void loadUsers() {
        executeTask(
            () -> repository.getUsers(),
            users::postValue,
            exception -> error.postValue(exception.getMessage())
        );
    }

    public void refreshUsers() {
        executeTask(
            () -> repository.refreshUsers(),
            users::postValue,
            exception -> error.postValue("Failed to refresh users")
        );
    }
}
```

### Creating a Repository

```java
public class UserRepository extends BaseRepository {
    
    private final APIService apiService;

    public UserRepository() {
        this.apiService = RetrofitClient.getInstance().create(APIService.class);
    }

    public CompletableFuture<DataState<List<User>>> getUsers() {
        return getResult(() -> {
            // Perform API call
            Response<List<User>> response = apiService.getUsers().execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            } else {
                throw new RuntimeException("Failed to load users");
            }
        });
    }

    public CompletableFuture<DataState<User>> createUser(User user) {
        return getResult(() -> {
            Response<User> response = apiService.createUser(user).execute();
            if (response.isSuccessful() && response.body() != null) {
                return response.body();
            } else {
                throw new RuntimeException("Failed to create user");
            }
        });
    }
}
```

## 🔧 Base Classes Usage

### BaseActivity Features

- **Automatic View Binding**: No need for findViewById
- **Lifecycle Management**: Structured initialization flow
- **Type Safety**: Generic binding parameter

### BaseFragment Features

- **ViewModel Integration**: Automatic loading state observation
- **Loading Dialog**: Built-in loading state management
- **Lifecycle Awareness**: Proper observer setup

### BaseAdapter Features

- **Generic Implementation**: Works with any data type and binding
- **Automatic ViewHolder**: No need to create custom ViewHolders
- **Data Management**: Built-in methods for data manipulation

### BaseViewModel Features

- **Async Task Execution**: Simplified background operations
- **Loading State**: Automatic loading state management
- **Error Handling**: Standardized error handling pattern

### BaseRepository Features

- **Result Wrapping**: Automatic DataState wrapping
- **Thread Management**: Background execution handling
- **Exception Handling**: Consistent error management

## 📚 Examples

### Complete Feature Implementation

Here's how to implement a complete user management feature:

#### 1. Define API Endpoints

```java
// In APIService.java
@GET("users")
Call<List<User>> getUsers();

@POST("users")
Call<User> createUser(@Body User user);

@PUT("users/{id}")
Call<User> updateUser(@Path("id") int id, @Body User user);

@DELETE("users/{id}")
Call<Void> deleteUser(@Path("id") int id);
```

#### 2. Create Data Model

```java
public class User {
    private int id;
    private String name;
    private String email;
    private String avatarUrl;
    
    // Constructors, getters, and setters
}
```

#### 3. Implement Repository

```java
public class UserRepository extends BaseRepository {
    // Implementation as shown above
}
```

#### 4. Create ViewModel

```java
public class UserListViewModel extends BaseViewModel {
    // Implementation as shown above
}
```

#### 5. Implement Fragment

```java
public class UserListFragment extends BaseFragment<FragmentUserListBinding> {
    // Implementation as shown above
}
```

## 🎯 Best Practices

### Code Organization

- **Package by Feature**: Group related classes together
- **Consistent Naming**: Follow established naming conventions
- **Documentation**: Document all public methods and classes

### Performance

- **View Binding**: Always use view binding for type safety
- **RecyclerView**: Use BaseAdapter for consistent implementation
- **Memory Management**: Properly handle lifecycle in ViewModels

### Error Handling

- **DataState Pattern**: Always use DataState for operation results
- **User Feedback**: Provide meaningful error messages
- **Logging**: Log errors for debugging purposes

### Testing

- **Unit Tests**: Test ViewModels and Repositories
- **UI Tests**: Test user interactions and flows
- **Mock Data**: Use mock repositories for testing

## 🔄 Configuration

### API Configuration

Update `APIConstant.java` with your API details:

```java
public class APIConstant {
    public static final String BASE_URL = "https://your-api.com/api/v1/";
    
    public static class EndPoint {
        public static final String USERS = "users";
        public static final String LOGIN = "auth/login";
        // Add your endpoints here
    }
    
    public static class TimeOut {
        public static final long CONNECT_TIME_OUT = 30L;
        public static final long READ_TIME_OUT = 30L;
        public static final long WRITE_TIME_OUT = 30L;
    }
}
```

### Dependencies

The project includes these key dependencies:

- **View Binding**: For type-safe view access
- **Navigation Component**: For fragment navigation
- **ViewModel & LiveData**: For MVVM architecture
- **Retrofit**: For network operations
- **Glide**: For image loading
- **Room**: For local database (ready to use)
- **Lottie**: For animations

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

If you encounter any issues or have questions:

1. Check the [Issues](../../issues) section
2. Review the comprehensive code documentation
3. Refer to the implementation examples above

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [QUICK_START.md](QUICK_START.md) | Quick start guide and testing |
| [FIREBASE_SETUP.md](FIREBASE_SETUP.md) | Complete Firebase configuration guide |
| [PROJECT_STATUS.md](PROJECT_STATUS.md) | Current implementation status and roadmap |
| [WELCOME_SCREEN.md](WELCOME_SCREEN.md) | Onboarding screen documentation |
| [LOGIN_IMPLEMENTATION.md](LOGIN_IMPLEMENTATION.md) | Login system technical details |
| [SETUP_CHECKLIST.md](SETUP_CHECKLIST.md) | Step-by-step setup checklist |

## 🚀 Roadmap

### ✅ Phase 1: Authentication (Week 1) - IN PROGRESS
- [x] Welcome/Onboarding Screen
- [x] Login Screen
- [ ] Register Screen
- [ ] Forgot Password

### 📋 Phase 2: Core App (Week 2-3)
- [ ] Dashboard Home (charts, metrics overview)
- [ ] Profile Screen
- [ ] Settings Screen
- [ ] Navigation Bottom Bar

### 📊 Phase 3: Health Features (Week 4-6)
- [ ] Health Metrics (5 screens)
- [ ] Reminders System (4 screens)
- [ ] Medical Records (5 screens)
- [ ] Medicines Management (2 screens)

### 🚀 Phase 4: Advanced (Week 7-9)
- [ ] OCR Medicine Scanning (ML Kit)
- [ ] AI Insights & Analytics
- [ ] PDF Report Export
- [ ] Offline Sync

### 🎯 Phase 5: Polish (Week 10)
- [ ] Animations & Transitions
- [ ] Testing & Bug Fixes
- [ ] Performance Optimization
- [ ] Production Release

## 🆘 Support & Contributing

**Need Help?**
- Check the documentation files above
- Review [PROJECT_STATUS.md](PROJECT_STATUS.md) for current progress
- Check issues on GitHub

**Contributing:**
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push and create a Pull Request

## 📄 License

This project is for educational purposes (Final Android Project).

---

**Built with ❤️ by Van Anh**  
**Status**: 🚧 In Development (8% complete)  
**Next**: Register Screen + Firebase Setup

Happy coding! 🎉