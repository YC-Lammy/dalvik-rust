package org.xmlpull.v1;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public interface XmlSerializer {
    public abstract XmlSerializer attribute (String namespace, 
                String name, 
                String value) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void cdsect (String text) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void comment (String text) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void docdecl (String text) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void endDocument () throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract XmlSerializer endTag (String namespace, 
                String name) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void entityRef (String text) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void flush () throws IOException;

    public abstract int getDepth ();

    public abstract boolean getFeature (String name) throws IllegalArgumentException;

    public abstract String getName ();

    public abstract String getNamespace ();

    public abstract String getPrefix (String namespace, 
                boolean generatePrefix) throws IllegalArgumentException;

    public abstract Object getProperty (String name);

    public abstract void ignorableWhitespace (String text) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void processingInstruction (String text) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void setFeature (String name, 
                boolean state) throws IllegalArgumentException, IllegalStateException;

    public abstract void setOutput (OutputStream os, 
                String encoding) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void setOutput (Writer writer) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void setPrefix (String prefix, String namespace) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract void setProperty (String name, Object value) throws IllegalArgumentException, IllegalStateException;

    public abstract void startDocument (String encoding, Boolean standalone) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract XmlSerializer startTag (String namespace, String name) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract XmlSerializer text (char[] buf, int start, int len) throws IOException, IllegalArgumentException, IllegalStateException;

    public abstract XmlSerializer text (String text) throws IOException, IllegalArgumentException, IllegalStateException;
}
