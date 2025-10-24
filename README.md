# Base - Android MVVM Foundation Project

A well-structured Android application foundation built with MVVM architecture, providing reusable base classes and comprehensive documentation for rapid development.

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

Base is a production-ready Android foundation project that implements:

- **MVVM Architecture Pattern** with LiveData and ViewModel
- **View Binding** for type-safe view access
- **Repository Pattern** for data management
- **Result Pattern** with DataState for error handling
- **Retrofit Integration** for network operations
- **Comprehensive Documentation** for all components

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

## 🚀 Getting Started

### 1. Clone and Setup

```bash
# Clone the repository
git clone <repository-url>
cd Base

# Open in Android Studio
# File → Open → Select the Base folder
```

### 2. Build the Project

```bash
# Clean and build
./gradlew clean
./gradlew assembleDebug

# Or use Android Studio
# Build → Clean Project
# Build → Rebuild Project
```

### 3. Run the Application

```bash
# Install on connected device/emulator
./gradlew installDebug

# Or use Android Studio
# Run → Run 'app'
```

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

## 🚀 Next Steps

After setting up the Base project:

1. **Configure API endpoints** in `APIConstant.java`
2. **Create your data models** following the User example
3. **Implement your repositories** extending BaseRepository
4. **Build your ViewModels** extending BaseViewModel
5. **Create your UI** using BaseActivity and BaseFragment
6. **Add your business logic** following the established patterns

Happy coding! 🎉