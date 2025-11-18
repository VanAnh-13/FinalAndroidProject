#!/usr/bin/env python3
"""
Test script for Android Studio MCP Server
"""

import sys
import os

# Add parent directory to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

def test_imports():
    """Test if all required imports work"""
    print("Testing imports...")
    try:
        from mcp.server import Server
        from mcp.server.stdio import stdio_server
        from mcp.types import Tool, TextContent
        print("✓ MCP SDK imports successful")
        return True
    except ImportError as e:
        print(f"✗ Import error: {e}")
        print("\nPlease install MCP SDK:")
        print("  pip install mcp")
        return False

def test_project_structure():
    """Test if project structure is valid"""
    print("\nTesting project structure...")
    
    # Get project root (parent of mcp-server directory)
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    
    print(f"Project root: {project_root}")
    
    # Check for key files
    checks = [
        ("gradlew.bat", "Gradle wrapper"),
        ("app/build.gradle", "App build.gradle"),
        ("app/src/main/AndroidManifest.xml", "AndroidManifest.xml"),
    ]
    
    all_passed = True
    for file_path, description in checks:
        full_path = os.path.join(project_root, file_path)
        if os.path.exists(full_path):
            print(f"✓ Found {description}")
        else:
            print(f"✗ Missing {description}: {file_path}")
            all_passed = False
    
    return all_passed

def test_server_creation():
    """Test if server can be created"""
    print("\nTesting server creation...")
    try:
        from android_studio_mcp_server import AndroidStudioMCPServer
        
        # Get project root
        script_dir = os.path.dirname(os.path.abspath(__file__))
        project_root = os.path.dirname(script_dir)
        
        server = AndroidStudioMCPServer(project_root)
        print("✓ Server created successfully")
        print(f"  Project root: {server.project_root}")
        return True
    except Exception as e:
        print(f"✗ Server creation failed: {e}")
        return False

def main():
    """Run all tests"""
    print("=" * 50)
    print("Android Studio MCP Server - Test Suite")
    print("=" * 50)
    
    results = []
    
    # Run tests
    results.append(("Imports", test_imports()))
    results.append(("Project Structure", test_project_structure()))
    results.append(("Server Creation", test_server_creation()))
    
    # Print summary
    print("\n" + "=" * 50)
    print("Test Summary")
    print("=" * 50)
    
    passed = sum(1 for _, result in results if result)
    total = len(results)
    
    for test_name, result in results:
        status = "✓ PASS" if result else "✗ FAIL"
        print(f"{status}: {test_name}")
    
    print(f"\nTotal: {passed}/{total} tests passed")
    
    if passed == total:
        print("\n✓ All tests passed! Server is ready to use.")
        print("\nNext steps:")
        print("1. Restart Kiro IDE")
        print("2. The server will automatically connect")
        print("3. Try asking Kiro to read a file from your project")
        return 0
    else:
        print("\n✗ Some tests failed. Please fix the issues above.")
        return 1

if __name__ == "__main__":
    sys.exit(main())
