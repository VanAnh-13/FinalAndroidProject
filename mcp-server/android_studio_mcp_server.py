#!/usr/bin/env python3
"""
Android Studio MCP Server
Provides tools for Kiro to interact with Android Studio projects
"""

import asyncio
import json
import os
import subprocess
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional

# MCP SDK imports
try:
    from mcp.server import Server
    from mcp.server.stdio import stdio_server
    from mcp.types import Tool, TextContent, ImageContent, EmbeddedResource
except ImportError:
    print("Error: MCP SDK not installed. Install with: pip install mcp", file=sys.stderr)
    sys.exit(1)


class AndroidStudioMCPServer:
    """MCP Server for Android Studio integration"""
    
    def __init__(self, project_root: str):
        self.project_root = Path(project_root).resolve()
        self.server = Server("android-studio-mcp")
        self.setup_handlers()
        
    def setup_handlers(self):
        """Setup MCP tool handlers"""
        
        @self.server.list_tools()
        async def list_tools() -> List[Tool]:
            """List available tools"""
            return [
                Tool(
                    name="read_file",
                    description="Read a file from the Android project",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "path": {
                                "type": "string",
                                "description": "Relative path to file from project root"
                            }
                        },
                        "required": ["path"]
                    }
                ),
                Tool(
                    name="write_file",
                    description="Write content to a file in the Android project",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "path": {
                                "type": "string",
                                "description": "Relative path to file from project root"
                            },
                            "content": {
                                "type": "string",
                                "description": "Content to write to the file"
                            }
                        },
                        "required": ["path", "content"]
                    }
                ),
                Tool(
                    name="list_files",
                    description="List files in a directory",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "path": {
                                "type": "string",
                                "description": "Relative path to directory from project root",
                                "default": "."
                            },
                            "pattern": {
                                "type": "string",
                                "description": "File pattern to match (e.g., '*.java', '*.xml')",
                                "default": "*"
                            }
                        }
                    }
                ),
                Tool(
                    name="run_gradle_task",
                    description="Run a Gradle task",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "task": {
                                "type": "string",
                                "description": "Gradle task to run (e.g., 'assembleDebug', 'test', 'clean')"
                            },
                            "args": {
                                "type": "array",
                                "items": {"type": "string"},
                                "description": "Additional arguments for the task",
                                "default": []
                            }
                        },
                        "required": ["task"]
                    }
                ),
                Tool(
                    name="search_code",
                    description="Search for code patterns in the project",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "query": {
                                "type": "string",
                                "description": "Search query (regex pattern)"
                            },
                            "file_pattern": {
                                "type": "string",
                                "description": "File pattern to search in (e.g., '*.java')",
                                "default": "*.java"
                            },
                            "path": {
                                "type": "string",
                                "description": "Directory to search in",
                                "default": "app/src"
                            }
                        },
                        "required": ["query"]
                    }
                ),
                Tool(
                    name="get_project_structure",
                    description="Get the project structure and key files",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "depth": {
                                "type": "integer",
                                "description": "Maximum depth to traverse",
                                "default": 3
                            }
                        }
                    }
                ),
                Tool(
                    name="analyze_dependencies",
                    description="Analyze project dependencies from build.gradle",
                    inputSchema={
                        "type": "object",
                        "properties": {}
                    }
                ),
                Tool(
                    name="get_build_status",
                    description="Get the current build status and recent build logs",
                    inputSchema={
                        "type": "object",
                        "properties": {}
                    }
                ),
                Tool(
                    name="run_tests",
                    description="Run unit tests for the project",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "test_class": {
                                "type": "string",
                                "description": "Specific test class to run (optional)"
                            },
                            "test_method": {
                                "type": "string",
                                "description": "Specific test method to run (optional)"
                            }
                        }
                    }
                ),
                Tool(
                    name="get_logcat",
                    description="Get Android logcat output",
                    inputSchema={
                        "type": "object",
                        "properties": {
                            "filter": {
                                "type": "string",
                                "description": "Logcat filter (e.g., 'tag:priority')",
                                "default": "*:I"
                            },
                            "lines": {
                                "type": "integer",
                                "description": "Number of lines to retrieve",
                                "default": 100
                            }
                        }
                    }
                )
            ]
        
        @self.server.call_tool()
        async def call_tool(name: str, arguments: Dict[str, Any]) -> List[TextContent]:
            """Handle tool calls"""
            
            try:
                if name == "read_file":
                    return await self.read_file(arguments["path"])
                    
                elif name == "write_file":
                    return await self.write_file(arguments["path"], arguments["content"])
                    
                elif name == "list_files":
                    path = arguments.get("path", ".")
                    pattern = arguments.get("pattern", "*")
                    return await self.list_files(path, pattern)
                    
                elif name == "run_gradle_task":
                    task = arguments["task"]
                    args = arguments.get("args", [])
                    return await self.run_gradle_task(task, args)
                    
                elif name == "search_code":
                    query = arguments["query"]
                    file_pattern = arguments.get("file_pattern", "*.java")
                    path = arguments.get("path", "app/src")
                    return await self.search_code(query, file_pattern, path)
                    
                elif name == "get_project_structure":
                    depth = arguments.get("depth", 3)
                    return await self.get_project_structure(depth)
                    
                elif name == "analyze_dependencies":
                    return await self.analyze_dependencies()
                    
                elif name == "get_build_status":
                    return await self.get_build_status()
                    
                elif name == "run_tests":
                    test_class = arguments.get("test_class")
                    test_method = arguments.get("test_method")
                    return await self.run_tests(test_class, test_method)
                    
                elif name == "get_logcat":
                    filter_str = arguments.get("filter", "*:I")
                    lines = arguments.get("lines", 100)
                    return await self.get_logcat(filter_str, lines)
                    
                else:
                    return [TextContent(type="text", text=f"Unknown tool: {name}")]
                    
            except Exception as e:
                return [TextContent(type="text", text=f"Error executing {name}: {str(e)}")]
    
    async def read_file(self, path: str) -> List[TextContent]:
        """Read a file from the project"""
        file_path = self.project_root / path
        
        if not file_path.exists():
            return [TextContent(type="text", text=f"File not found: {path}")]
        
        if not file_path.is_file():
            return [TextContent(type="text", text=f"Not a file: {path}")]
        
        try:
            content = file_path.read_text(encoding='utf-8')
            return [TextContent(type="text", text=content)]
        except Exception as e:
            return [TextContent(type="text", text=f"Error reading file: {str(e)}")]
    
    async def write_file(self, path: str, content: str) -> List[TextContent]:
        """Write content to a file"""
        file_path = self.project_root / path
        
        try:
            # Create parent directories if they don't exist
            file_path.parent.mkdir(parents=True, exist_ok=True)
            
            # Write the file
            file_path.write_text(content, encoding='utf-8')
            
            return [TextContent(type="text", text=f"Successfully wrote to {path}")]
        except Exception as e:
            return [TextContent(type="text", text=f"Error writing file: {str(e)}")]
    
    async def list_files(self, path: str, pattern: str) -> List[TextContent]:
        """List files in a directory"""
        dir_path = self.project_root / path
        
        if not dir_path.exists():
            return [TextContent(type="text", text=f"Directory not found: {path}")]
        
        if not dir_path.is_dir():
            return [TextContent(type="text", text=f"Not a directory: {path}")]
        
        try:
            files = list(dir_path.glob(pattern))
            file_list = "\n".join([str(f.relative_to(self.project_root)) for f in files])
            
            return [TextContent(type="text", text=f"Files matching '{pattern}' in {path}:\n{file_list}")]
        except Exception as e:
            return [TextContent(type="text", text=f"Error listing files: {str(e)}")]
    
    async def run_gradle_task(self, task: str, args: List[str]) -> List[TextContent]:
        """Run a Gradle task"""
        try:
            # Determine the Gradle wrapper command
            if sys.platform == "win32":
                gradle_cmd = str(self.project_root / "gradlew.bat")
            else:
                gradle_cmd = str(self.project_root / "gradlew")
            
            # Build the command
            cmd = [gradle_cmd, task] + args
            
            # Run the command
            process = await asyncio.create_subprocess_exec(
                *cmd,
                cwd=str(self.project_root),
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )
            
            stdout, stderr = await process.communicate()
            
            output = stdout.decode('utf-8', errors='replace')
            if stderr:
                output += "\n\nErrors:\n" + stderr.decode('utf-8', errors='replace')
            
            return [TextContent(type="text", text=f"Gradle task '{task}' output:\n{output}")]
            
        except Exception as e:
            return [TextContent(type="text", text=f"Error running Gradle task: {str(e)}")]
    
    async def search_code(self, query: str, file_pattern: str, path: str) -> List[TextContent]:
        """Search for code patterns"""
        search_path = self.project_root / path
        
        if not search_path.exists():
            return [TextContent(type="text", text=f"Path not found: {path}")]
        
        try:
            import re
            pattern = re.compile(query)
            results = []
            
            for file_path in search_path.rglob(file_pattern):
                if file_path.is_file():
                    try:
                        content = file_path.read_text(encoding='utf-8')
                        matches = pattern.finditer(content)
                        
                        for match in matches:
                            line_num = content[:match.start()].count('\n') + 1
                            results.append(f"{file_path.relative_to(self.project_root)}:{line_num}: {match.group()}")
                    except:
                        continue
            
            if results:
                return [TextContent(type="text", text=f"Search results for '{query}':\n" + "\n".join(results[:50]))]
            else:
                return [TextContent(type="text", text=f"No matches found for '{query}'")]
                
        except Exception as e:
            return [TextContent(type="text", text=f"Error searching code: {str(e)}")]
    
    async def get_project_structure(self, depth: int) -> List[TextContent]:
        """Get project structure"""
        try:
            def build_tree(path: Path, current_depth: int, max_depth: int, prefix: str = "") -> List[str]:
                if current_depth > max_depth:
                    return []
                
                items = []
                try:
                    entries = sorted(path.iterdir(), key=lambda x: (not x.is_dir(), x.name))
                    for i, entry in enumerate(entries):
                        if entry.name.startswith('.'):
                            continue
                        
                        is_last = i == len(entries) - 1
                        current_prefix = "└── " if is_last else "├── "
                        items.append(f"{prefix}{current_prefix}{entry.name}")
                        
                        if entry.is_dir() and current_depth < max_depth:
                            next_prefix = prefix + ("    " if is_last else "│   ")
                            items.extend(build_tree(entry, current_depth + 1, max_depth, next_prefix))
                except PermissionError:
                    pass
                
                return items
            
            tree = [str(self.project_root.name) + "/"]
            tree.extend(build_tree(self.project_root, 0, depth))
            
            return [TextContent(type="text", text="Project Structure:\n" + "\n".join(tree))]
            
        except Exception as e:
            return [TextContent(type="text", text=f"Error getting project structure: {str(e)}")]
    
    async def analyze_dependencies(self) -> List[TextContent]:
        """Analyze project dependencies"""
        try:
            build_gradle = self.project_root / "app" / "build.gradle"
            
            if not build_gradle.exists():
                return [TextContent(type="text", text="build.gradle not found")]
            
            content = build_gradle.read_text(encoding='utf-8')
            
            # Extract dependencies
            import re
            dep_pattern = r"implementation\s+['\"]([^'\"]+)['\"]"
            dependencies = re.findall(dep_pattern, content)
            
            dep_list = "\n".join([f"- {dep}" for dep in dependencies])
            
            return [TextContent(type="text", text=f"Project Dependencies:\n{dep_list}")]
            
        except Exception as e:
            return [TextContent(type="text", text=f"Error analyzing dependencies: {str(e)}")]
    
    async def get_build_status(self) -> List[TextContent]:
        """Get build status"""
        try:
            build_dir = self.project_root / "app" / "build"
            
            if not build_dir.exists():
                return [TextContent(type="text", text="No build directory found. Project may not have been built yet.")]
            
            # Check for APK files
            apk_dir = build_dir / "outputs" / "apk"
            apks = list(apk_dir.rglob("*.apk")) if apk_dir.exists() else []
            
            status = "Build Status:\n"
            if apks:
                status += f"✓ Found {len(apks)} APK(s):\n"
                for apk in apks:
                    status += f"  - {apk.relative_to(self.project_root)}\n"
            else:
                status += "✗ No APK files found\n"
            
            return [TextContent(type="text", text=status)]
            
        except Exception as e:
            return [TextContent(type="text", text=f"Error getting build status: {str(e)}")]
    
    async def run_tests(self, test_class: Optional[str], test_method: Optional[str]) -> List[TextContent]:
        """Run unit tests"""
        try:
            task = "test"
            args = []
            
            if test_class:
                args.append(f"--tests {test_class}")
                if test_method:
                    args[-1] += f".{test_method}"
            
            return await self.run_gradle_task(task, args)
            
        except Exception as e:
            return [TextContent(type="text", text=f"Error running tests: {str(e)}")]
    
    async def get_logcat(self, filter_str: str, lines: int) -> List[TextContent]:
        """Get Android logcat output"""
        try:
            cmd = ["adb", "logcat", "-t", str(lines), filter_str]
            
            process = await asyncio.create_subprocess_exec(
                *cmd,
                stdout=asyncio.subprocess.PIPE,
                stderr=asyncio.subprocess.PIPE
            )
            
            stdout, stderr = await process.communicate()
            
            if process.returncode != 0:
                return [TextContent(type="text", text=f"Error: {stderr.decode('utf-8', errors='replace')}")]
            
            output = stdout.decode('utf-8', errors='replace')
            return [TextContent(type="text", text=f"Logcat output:\n{output}")]
            
        except FileNotFoundError:
            return [TextContent(type="text", text="Error: adb not found. Make sure Android SDK is installed and adb is in PATH.")]
        except Exception as e:
            return [TextContent(type="text", text=f"Error getting logcat: {str(e)}")]
    
    async def run(self):
        """Run the MCP server"""
        async with stdio_server() as (read_stream, write_stream):
            await self.server.run(
                read_stream,
                write_stream,
                self.server.create_initialization_options()
            )


def main():
    """Main entry point"""
    # Get project root from command line or environment variable
    project_root = sys.argv[1] if len(sys.argv) > 1 else os.getcwd()
    
    # Create and run the server
    server = AndroidStudioMCPServer(project_root)
    asyncio.run(server.run())


if __name__ == "__main__":
    main()
