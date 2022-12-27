## Setup
If vscode does not detect the references libraries, they are all included in the itext7-core-... folder

## Limitations
There appears to be a bug in iText: for a piece of text made italic using setItalic(), an extra space appears to be added
at the end, which somewhat messes up the formatting.

## Extensions
### Refactoring
- parseCommand() in PDFFactory: split commands into either text or paragraph formatting options using a set and fully delegate the responsibility of parsing to the actual TextFactory and ParagraphFactory classes.
- Changing the input handling to not consider the whole input at once and split by paragraph instead first and use a buffer to more effectively handle larger input files. Additionally, regex could be used to find commands rather than parsing individual words once by one, but taking arguments is more complicated this way and may not result in better performance, but is a possible approach.
- formatting options for text and paragraph could be added as separate classes if there were a higher number of options to reduce the number of class variables needed.
- possibly worth adding a trim() to the first string of every paragraph for correctness' sake, but this does not seem to affect the formatting so I did not include it as it somewhat awkward.