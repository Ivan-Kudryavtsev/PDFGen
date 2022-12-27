import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;

import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;


class TextFactory {
    private String text;
    // Additional formatting options could be added here or as a separate class
    private String fontstyle;
    private int fontsize = 12;

    // Initialise as empty string: initialisation as null leads to odd behaviour with appending
    public TextFactory() {
        this.text = "";
    }

    public TextFactory(String t) {
        this.text = t;
    }

    public String getText() {
        return text;
    }

    public void setText(String t) {
        this.text = t;
    }

    public void appendText(String t) {
        this.text += t;
    }

    public void clearText() {
        this.text = "";
    }

    public String getFontstyle() {
        return fontstyle;
    }

    public void setFontstyle(String fontstyle) {
        this.fontstyle = fontstyle;
    }

    public int getFontsize() {
        return fontsize;
    }

    public void setFontsize(int fontsize) {
        this.fontsize = fontsize;
    }

    public void setSize(String t) {
        switch (t) {
            case "large":
                setFontsize(24);
                break;
            case "regular":
                setFontsize(12);
                break;
            case "small":
                setFontsize(8);
                break;
        }
    }

    // Function to change the relevant parameter when given a command
    public void parseCommand(String cmd) {
        cmd = cmd.substring(1).trim();
        switch (cmd) {
            case "large":
                this.setSize("large");
                break;
            case "small":
                this.setSize("small");
                break;
            case "normal":
                this.setSize("regular");
                break;
            case "italics":
                this.setFontstyle("italic");
                break;
            case "bold":
                this.setFontstyle("bold");
                break;
            case "regular":
                this.setFontstyle("regular");
                break;
        }
    }

    // Function to return a Text object based on factory parameters
    public Text generateText() {
        if (text.equals("")) {
            return null;
        }

        // Remove trailing whitespace before punctuation
        String string = text.trim();
        if (Character.isAlphabetic(string.charAt(0)) || Character.isDigit(string.charAt(0))){
            string = " " + string;
        }

        // Set parameters
        Text r = new Text(string);
        if (fontstyle != null) {
            if (fontstyle.equals("bold")) {
                r.setBold();
            }
            if (fontstyle.equals("italic")) {
                r.setItalic();
            }
        }

        r.setFontSize(fontsize);
        // Reset text after generating object
        clearText();

        // Return Text object
        return r;
    }
}

class ParagraphFactory {
    private Paragraph paragraph;
    // Margin and prevmargin are used to alter the indentation
    private int margin;
    private int prevmargin = 0;

    private TextAlignment alignment;

    public ParagraphFactory() {
        paragraph = new Paragraph();
    }

    public void parseCommand(String cmd) {
        switch (cmd) {
            case ".fill":
                setAlignment(TextAlignment.JUSTIFIED);
                break;
            case ".nofill":
                setAlignment(TextAlignment.LEFT);
                break;
        }
    }

    public void addText(Text t) {
        paragraph.add(t);
    }

    public void clearParagraph() {
        paragraph = new Paragraph();
    }

    public void setMargin(int a) {
        margin = (prevmargin + a) * 10;
        prevmargin = margin / 10;
    }

    public void setAlignment(TextAlignment a) {
        alignment = a;
    }

    public Paragraph generateParagraph() {
        if (paragraph.isEmpty()) {
            return null;
        }
        // Set paragraph parameters
        paragraph.setTextAlignment(alignment);
        paragraph.setMarginLeft(margin);
        // Clear previous paragraph
        Paragraph n = paragraph;
        clearParagraph();
        // Return
        return n;
    }

}

class PDFFactory {
    private TextFactory tf;
    private ParagraphFactory pf;
    private Document doc;

    public PDFFactory(PdfDocument pdfDocument) {
        tf = new TextFactory();
        pf = new ParagraphFactory();
        doc = new Document(pdfDocument);
    }

    public void addWord(String word) {
        tf.appendText(word + " ");
    }

    private Paragraph generateParagraph() {
        // Finish generating Text object if there is any left.
        Text t = tf.generateText();
        // Add if necessary
        if (t != null) {
            pf.addText(t);
        }
        // Generate the paragraph
        return pf.generateParagraph();
    }

    public void parseCommand(String cmd, String[] args) {
        // There are two types of command: one for paragraph formatting options, and one for text.
        // Additionally, there may be commands with arguments.

        // There is also the special case of .paragraph which generates the previuos paragraph.

        // Declare a blank paragraph.
        Paragraph p = null;

        // Args exist: right now there are no commands with args other than .indent
        if (args != null) {
            if (cmd.equals(".indent")) {
                p = generateParagraph();
                // Get the margin amount from args and set in factory object
                int amount = Integer.valueOf(args[0]);
                pf.setMargin(amount);
            }
            if (p != null)
                doc.add(p);
            return;
        }

        // There are only 3 paragraph commands with no arguments at the moment:
        // ideally there would be some fixed sets with commands partitioned between the two to choose between
        if (cmd.equals(".paragraph")) {
            p = generateParagraph();
        } else if (cmd.equals(".fill") || cmd.equals(".nofill")) {
            p = generateParagraph();
            pf.parseCommand(cmd);
        } else {
            // handle Text command
            Text t = tf.generateText();
            tf.parseCommand(cmd);
            if (t != null) {
                pf.addText(t);
            }
        }

        if (p != null)
            doc.add(p);
    }

    public void generatePDF() {
        // Finish generating last paragraph and close the document.
        Paragraph p = generateParagraph();
        if (p != null)
            doc.add(p);
        doc.close();
    }
}

public class PDFgen {
    public static void main(String[] args) {

        // Open file
        String fname = "testdatawithcommands.txt";
        BufferedReader in = getFileHandler(fname);

        // Handle failure
        if (in == null) {
            System.out.println("Failed to open file.");
            return;
        }

        // Read in each line and construct a string containing the file contents
        StringBuilder s = new StringBuilder();
        try {
            String line = in.readLine();
            while (line != null) {
                s.append(line + " ");
                line = in.readLine();
            }
        } catch (Exception e) {
            System.err.println("IO exception");
            e.printStackTrace();
        }

        String documentString = s.toString();
        String destfname = "output.pdf";

        // Define argument count per instruction: for a longer set of arguments this
        // should move to a PDFFactory function
        HashMap<String, Integer> arguments = new HashMap();
        arguments.put(".indent", 1);

        // Working with iText requires a try catch clause
        try {
            // Initialise iText pdf writing objects
            PdfWriter writer = new PdfWriter(destfname);
            PdfDocument pdfdoc = new PdfDocument(writer);

            // Initialise
            PDFFactory wrapper = new PDFFactory(pdfdoc);

            // Split string into individual words for easier processing
            String[] strings = documentString.split(" ");

            // Process each string either as command word or as part of document
            for (int i = 0; i < strings.length; i++) {
                String a = strings[i];

                // Not a command: added immediately to printout
                if (!a.startsWith(".")) {
                    wrapper.addWord(a);
                    continue;
                }

                // If the command has argument count listed, then read in the corresponding
                // number of arguments
                if (arguments.containsKey(a)) {
                    int argNumber = arguments.get(a);
                    String[] argArray = new String[argNumber];
                    for (int j = 0; j < argNumber; j++) {
                        argArray[j] = strings[++i];
                    }
                    // Pass command and args
                    wrapper.parseCommand(a, argArray);
                } else {
                    // Otherwise, pass the command in as is
                    wrapper.parseCommand(a, null);
                }
            }

            wrapper.generatePDF();

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }

    static BufferedReader getFileHandler(String fname) {
        // Create bufferedreader object for the filename specified.
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(fname));
        } catch (Exception e) {
            // FileReader throws FileNotFoundException
            System.err.println("Filename not found.");
            e.printStackTrace();
        }
        return in;
    }
}
