# File Parsing System

## Overview

The Lost & Found application uses a flexible, extensible file parsing system built with Object-Oriented design patterns. Currently supports **PDF files only**, but designed for easy extension.

## Architecture

### Design Patterns Used

1. **Strategy Pattern** - Different parsing strategies for different file types
2. **Factory Pattern** - Automatic strategy selection based on file type
3. **Template Method Pattern** - Common parsing workflow with customizable steps

### Core Components

```
FileParsingStrategy (Interface)
├── AbstractFileParsingStrategy (Abstract Base Class)
    ├── PdfParsingStrategy (PDF Implementation)
    └── [Future: JsonParsingStrategy, TxtParsingStrategy, etc.]

FileParsingStrategyFactory (Factory)
├── Auto-discovers all strategy implementations
└── Selects appropriate strategy based on file type/extension

Exception Package
├── FileParsingException (Parsing errors)
└── UnsupportedFileTypeException (No strategy found)
```

## Current Implementation: PDF Parser

### Supported Format

The PDF parser expects a simple format with one item per line:
```
item_name quantity location
```

### Examples

**Simple Format:**
```
MacBook Pro 14-inch 1 Computer Lab Room 205
iPhone 15 2 Library Main Floor
USB Flash Drive 5 Computer Lab Room 103
```

**Header lines are automatically skipped:**
```
Lost Items Report
=================
item_name quantity location
MacBook Pro 1 Lab Room 205
iPhone 2 Main Floor
```

### Features

- ✅ Automatic header detection and skipping
- ✅ Robust error handling for invalid lines
- ✅ File size validation (10MB max)
- ✅ Empty file detection
- ✅ Detailed logging for debugging

## Adding New File Types

### Step 1: Create New Strategy

```java
@Slf4j
@Component
public class TxtParsingStrategy extends AbstractFileParsingStrategy {

    @Override
    protected List<LostItem> doParseFile(MultipartFile file) throws Exception {
        // Your parsing logic here
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        return parseContent(content);
    }

    @Override
    protected List<String> getSupportedExtensions() {
        return List.of(".txt", ".text");
    }

    @Override
    protected List<String> getSupportedMimeTypes() {
        return List.of("text/plain");
    }

    @Override
    public String getStrategyName() {
        return "Text File Parser";
    }

    private List<LostItem> parseContent(String content) throws FileParsingException {
        // Implementation specific to your format
        // Use parent methods: validateItemData(), createLostItem()
    }
}
```

### Step 2: That's It! 

The factory automatically discovers your new strategy via Spring's component scanning. No configuration needed.

## Provided Utilities (AbstractFileParsingStrategy)

### Validation
```java
protected void validateItemData(String itemName, Integer quantity, String place) throws FileParsingException
```

### Item Creation
```java
protected LostItem createLostItem(String itemName, int quantity, String place)
protected LostItem createLostItem(String itemName, int quantity, String place, String description)
```

### File Validation
```java
protected void validateFile(MultipartFile file) throws FileParsingException  // Size, null, empty checks
protected long getMaxFileSize()                                              // Override to customize (default 10MB)
```

## Error Handling

### Built-in Exceptions (in `com.example.lostfound.exception` package)
- `FileParsingException` - Parsing errors, invalid format
- `UnsupportedFileTypeException` - No strategy found for file type

### Automatic Error Handling
- Invalid lines are logged and skipped (not failed)
- Empty files throw exceptions
- Large files are rejected
- Corrupted files are handled gracefully

## API Usage

### Upload Endpoint
```http
POST /api/admin/upload
Content-Type: multipart/form-data

file: [PDF file]
```

### Response
```json
{
  "message": "File uploaded and processed successfully",
  "itemsCount": 5,
  "items": [...]
}
```

## Example Test Data

See `sample-data/sample_lost_items.txt` for format examples.

For testing, create a PDF with content like:
```
MacBook Pro 1 Computer Lab Room 205
iPhone 15 2 Library Main Floor
USB Flash Drive 5 Computer Lab Room 103
```

## Future Extensions

Commented example implementations available in:
- `JsonParsingStrategy.java` (for JSON files)

Simply uncomment and adjust as needed for new file types. 