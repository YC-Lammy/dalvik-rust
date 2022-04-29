package android.print;

import java.io.FileReader;
import java.io.File;
import java.io.FileNotFoundException;

public class PrintFSDocumentAdapter extends PrintDocumentAdapter{
    protected String mPath;
    protected FileReader mFile;

    public PrintFSDocumentAdapter(String path) throws FileNotFoundException{
        File f = new File(path);
        mFile = new FileReader(f);
        mPath = path;
    }

    @Override
    public void onStart() {
        
    }

    @Override
    public void onFinish() {
        
    }
    
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {

    };

    public void onWrite (PageRange[] pages, 
                ParcelFileDescriptor destination, 
                CancellationSignal cancellationSignal, 
                PrintDocumentAdapter.WriteResultCallback callback){

    };


}
