package android.print;

public abstract class PrintDocumentAdapter extends Object{
    public static final String EXTRA_PRINT_PREVIEW = "EXTRA_PRINT_PREVIEW";

    public void onFinish(){};
    public void onStart (){};

    public abstract void onLayout (PrintAttributes oldAttributes, 
                PrintAttributes newAttributes, 
                CancellationSignal cancellationSignal, 
                LayoutResultCallback callback, 
                Bundle extras);

    public abstract void onWrite (PageRange[] pages, 
                ParcelFileDescriptor destination, 
                CancellationSignal cancellationSignal, 
                PrintDocumentAdapter.WriteResultCallback callback);

}
