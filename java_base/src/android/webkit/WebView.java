package android.webkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.Writer;

import javax.swing.JPanel;
import java.util.random.RandomGenerator;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import java.net.ServerSocket;
import java.net.Socket;

import org.cef.CefApp;
import org.cef.CefApp.CefAppState;
import org.cef.CefClient;
import org.cef.CefSettings;
import org.cef.OS;
import org.cef.network.CefCookieManager;
import org.cef.network.CefCookie;
import org.cef.callback.CefCookieVisitor;
import org.cef.callback.CefPdfPrintCallback;
import org.cef.callback.CefQueryCallback;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.browser.CefMessageRouter;
import org.cef.browser.CefMessageRouter.CefMessageRouterConfig;
import org.cef.handler.CefAppHandlerAdapter;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefMessageRouterHandler;
import org.cef.misc.BoolRef;
import org.cef.misc.CefPdfPrintSettings;

import javafx.embed.swing.SwingNode;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;

import android.context.Context;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.autofill.AutofillValue;
import android.graphics.Bitmap;
import android.graphics.Picture;
import android.os.Handler;
import android.print.PrintDocumentAdapter;
import android.print.PrintFSDocumentAdapter;
import android.widget.AbsoluteLayout;

public class WebView extends AbsoluteLayout{

    public static final int RENDERER_PRIORITY_BOUND = 1;
    public static final int RENDERER_PRIORITY_IMPORTANT = 2;
    public static final int RENDERER_PRIORITY_WAIVED = 0;

    /**
     * URI scheme for telephone number.
     */
    public static final String SCHEME_TEL = "tel:";
    /**
     * URI scheme for email address.
     */
    public static final String SCHEME_MAILTO = "mailto:";
    /**
     * URI scheme for map address.
     */
    public static final String SCHEME_GEO = "geo:0,0?q=";



    public static interface FindListener{
        abstract void onFindResultReceived(int activeMatchOrdinal, int numberOfMatches, boolean isDoneCounting);
    }

    /* deprecated in API level 12
    public static interface PictureListener{

    }
    */

    public static class HitTestResult extends Object {
        public static final int AHCHOR_TYPE = 1;
        public static final int EDIT_TEXT_TYPE = 9;
        public static final int EMAIL_TYPE = 4;
        public static final int GEO_TYPE = 3;
        public static final int IMAGE_ANCHOR_TYPE = 6;
        public static final int IMAGE_TYPE = 5;
        public static final int PHONE_TYPE = 2;
        public static final int SRC_ANCHOR_TYPE = 7;
        public static final int SRC_IMAGE_ANCHOR_TYPE = 8;
        public static final int UNKNOWN_TYPE = 0;

        private String extra;
        private int type;

        public String getExtra(){
            return extra;
        }

        public int getType(){
            return type;
        }

        public HitTestResult(String extra_, int type_){
            extra = extra_;
            type = type_;
        }
    }

    /**
     * Callback interface supplied to {@link #postVisualStateCallback} for receiving
     * notifications about the visual state.
     */
    public static abstract class VisualStateCallback {
        /**
         * Invoked when the visual state is ready to be drawn in the next {@link #onDraw}.
         *
         * @param requestId The identifier passed to {@link #postVisualStateCallback} when this
         *                  callback was posted.
         */
        public abstract void onComplete(long requestId);
    }

    public class WebViewTransport {
        private WebView mWebview;

        /**
         * Sets the WebView to the transportation object.
         *
         * @param webview the WebView to transport
         */
        public synchronized void setWebView(WebView webview) {
            mWebview = webview;
        }

        /**
         * Gets the WebView object.
         *
         * @return the transported WebView object
         */
        public synchronized WebView getWebView() {
            return mWebview;
        }
    }

    class cookie_visitor implements CefCookieVisitor{
        @Override
        public boolean visit(CefCookie arg0, int arg1, int arg2, BoolRef arg3) {

            return false;
        }
    }

    class history extends WebHistoryItem{
        protected String mUrl;
        protected String mTitle;

        protected history(String url, String title){
            mUrl = url;
            mTitle = title;
        }

        @Override
        public Bitmap getFavicon() {
            try {
                String origin = getOriginalUrl();
                java.net.URI uri = new java.net.URI(origin + "/favicon.ico");
                
                java.net.http.HttpRequest.newBuilder()
                .GET()
                .uri(uri)
                .headers(
                    "Accept", "image/avif,image/webp,*/*",
                    "Accept-Encoding", "gzip, deflate, br",
                    "Accept-Language", "en-GB,en;q-0.5",
                    "Connection", "keep-alive",
                    "Host", uri.getHost(),
                    "User-Agent", "Mozilla/5.0"
                );

            } catch (Exception e) {
                return null;
            } 
        }

        @Override
        public String getOriginalUrl() {
            java.net.URI u = new java.net.URI(mUrl);
            return u.getScheme() + "://" +u.getHost();
        }

        @Override
        public String getTitle() {
            return mTitle;
        }

        @Override
        public String getUrl() {
            return mUrl;
        }

        @Override
        protected WebHistoryItem clone() {
            return new history(mUrl, mTitle);
        }
    }

    class historyList extends WebBackForwardList{

        protected ArrayList<history> mList;
        protected int mIndex;

        @Override
        public int getCurrentIndex() {
            return mIndex;
        }

        @Override
        public WebHistoryItem getCurrentItem() {
            if (mList.size() == 0){
                return null;
            }
            return mList.get(mIndex).clone();
        }

        @Override
        public WebHistoryItem getItemAtIndex(int index) {
            if (mList.size() <= index || index < 0){
                return null;
            }
            return mList.get(index).clone();
        }

        @Override
        public int getSize() {
            return mList.size();
        }

        @Override
        protected WebBackForwardList clone() {
            historyList l = new historyList();
            l.mIndex = mIndex;
            l.mList = new ArrayList<history>(mList);
            return l;
        }
    }

    private CefBrowser mBrowser;
    private SwingNode mUINode;

    private String mTitle;

    private historyList mForwardBackwardList;
    private boolean mIs_forward_or_bacward;

    private boolean mIs_match_marker_created;
    private ArrayList<FindListener> mFindListeners;

    private int mScrollX;
    private int mScrollY;

    public WebView(Context context) {
        this(context, null);
    }

    /**
     * Constructs a new WebView with layout parameters.
     *
     * @param context an Activity Context to access application assets
     * @param attrs an AttributeSet passed to our parent
     */
    public WebView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.webViewStyle);
    }

    /**
     * Constructs a new WebView with layout parameters and a default style.
     *
     * @param context an Activity Context to access application assets
     * @param attrs an AttributeSet passed to our parent
     * @param defStyleAttr an attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     */
    public WebView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    /**
     * Constructs a new WebView with layout parameters and a default style.
     *
     * @param context an Activity Context to access application assets
     * @param attrs an AttributeSet passed to our parent
     * @param defStyleAttr an attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @param defStyleRes a resource identifier of a style resource that
     *        supplies default values for the view, used only if
     *        defStyleAttr is 0 or can not be found in the theme. Can be 0
     *        to not look for defaults.
     */
    public WebView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        this(context, attrs, defStyleAttr, defStyleRes, null, false);
    }

    /**
     * Constructs a new WebView with layout parameters and a default style.
     *
     * @param context an Activity Context to access application assets
     * @param attrs an AttributeSet passed to our parent
     * @param defStyleAttr an attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @param privateBrowsing whether this WebView will be initialized in
     *                        private mode
     *
     * @deprecated Private browsing is no longer supported directly via
     * WebView and will be removed in a future release. Prefer using
     * {@link WebSettings}, {@link WebViewDatabase}, {@link CookieManager}
     * and {@link WebStorage} for fine-grained control of privacy data.
     */
    @Deprecated
    public WebView(Context context, AttributeSet attrs, int defStyleAttr,
            boolean privateBrowsing) {
        this(context, attrs, defStyleAttr, 0, null, privateBrowsing);
    }

    /**
     * Constructs a new WebView with layout parameters, a default style and a set
     * of custom JavaScript interfaces to be added to this WebView at initialization
     * time. This guarantees that these interfaces will be available when the JS
     * context is initialized.
     *
     * @param context an Activity Context to access application assets
     * @param attrs an AttributeSet passed to our parent
     * @param defStyleAttr an attribute in the current theme that contains a
     *        reference to a style resource that supplies default values for
     *        the view. Can be 0 to not look for defaults.
     * @param javaScriptInterfaces a Map of interface names, as keys, and
     *                             object implementing those interfaces, as
     *                             values
     * @param privateBrowsing whether this WebView will be initialized in
     *                        private mode
     * @hide This is used internally by dumprendertree, as it requires the JavaScript interfaces to
     *       be added synchronously, before a subsequent loadUrl call takes effect.
     */
    protected WebView(Context context, AttributeSet attrs, int defStyleAttr,
            Map<String, Object> javaScriptInterfaces, boolean privateBrowsing) {
        this(context, attrs, defStyleAttr, 0, javaScriptInterfaces, privateBrowsing);
    }

    /**
     * @hide
     */
    protected WebView(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes, Map<String, Object> javaScriptInterfaces,
            boolean privateBrowsing) {
        super(context, attrs, defStyleAttr, defStyleRes);

        if (context == null) {
            throw new IllegalArgumentException("Invalid context argument");
        }

        CefApp.addAppHandler(new CefAppHandlerAdapter(null) {});
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = false;
        //todo

        CefApp app = CefApp.getInstance(settings);
        CefClient client = app.createClient();
        client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onAddressChange(CefBrowser browser, CefFrame frame, String url) {
                if (!mIs_forward_or_bacward){
                    for (int i =mForwardBackwardList.getSize()-1; i > mForwardBackwardList.mIndex; i--){
                        mForwardBackwardList.mList.remove(i);
                    };
                    mForwardBackwardList.mList.add(new history(url, ""));
                    mForwardBackwardList.mIndex = mForwardBackwardList.getSize() -1;
                };
                mIs_forward_or_bacward = false;
            }

            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                mForwardBackwardList.mList.get(mForwardBackwardList.mIndex).mTitle = title;
            }
        });

        mBrowser = client.createBrowser("", false, false);
        mUINode = new SwingNode();

        JPanel panel = new JPanel();
        panel.add(mBrowser.getUIComponent());
        mUINode.setContent(panel);

        if (javaScriptInterfaces != null){
            for (Map.Entry<String, Object> entry : javaScriptInterfaces.entrySet()) {
                addJavascriptInterface(entry.getValue(), entry.getKey());
            }
        }

        mForwardBackwardList = new historyList();
        mFindListeners = new ArrayList<FindListener>();
        
    }

    public void addJavascriptInterface(Object object, String name){
        //todo
    }

    public void removeJavascriptInterface(String name) {
        //todo
    }

    public void autofill(SparseArray<AutofillValue> values){
        //todo
    }

    public boolean canGoBack(){
        return mForwardBackwardList.mIndex > 0;
    }

    public boolean canGoBackOrForward(int steps){
        return mForwardBackwardList.getSize() >= steps;
    }

    public boolean canGoForward(){
        return mForwardBackwardList.getSize() -  mForwardBackwardList.mIndex> 1;
    }

    public void goForward(){
        if (canGoForward()){
            mIs_forward_or_bacward = true;
            String uri = mForwardBackwardList.mList.get(mForwardBackwardList.mIndex +1).mUrl;
            mForwardBackwardList.mIndex += 1;
            mBrowser.loadURL(uri);
        }
    }

    public void goBack(){
        if (canGoBack()){
            mIs_forward_or_bacward = true;
            String uri = mForwardBackwardList.mList.get(mForwardBackwardList.mIndex -1).mUrl;
            mForwardBackwardList.mIndex -= 1;
            mBrowser.loadURL(uri);
        }
    }

    public boolean canZoomIn(){
        return true;
    }

    public boolean canZoomOut(){
        return true;
    }

    public Picture capturePicture(){
        BufferedImage pic = mBrowser.createScreenshot(true).get();
        javafx.scene.image.Image im = SwingFXUtils.toFXImage(pic, null);
        return new Picture(im);
    }

    public void clearCache(boolean includeDiskFiles){
        
    }

    static void clearClientCertPreferences(Runnable onCleared){
        
    }

    public void clearFormData(){

    }

    public void clearHistory() {
        
    }

    public void clearMatches(){
        if (mIs_match_marker_created){
            mBrowser.executeJavaScript("window.webview_native_match_marker.unmark();", "native/sclearMatches", 0);
        }
    }

    public void findAll(String find){
        if (!mIs_match_marker_created){
            mBrowser.executeJavaScript("/*!***************************************************" + 
            "* mark.js v8.11.1" +
            "* https://markjs.io/" +
            "* Copyright (c) 2014–2018, Julian Kühnel"+
            "* Released under the MIT license https://git.io/vwTVl"+
            "{!function(e,t){'object'==typeof exports&&'undefined'!=typeof module?module.exports=t():'function'==typeof define&&define.amd?define(t):e.Mark=t()}(this,function(){'use strict';var e='function'==typeof Symbol&&'symbol'==typeof Symbol.iterator?function(e){return typeof e}:function(e){return e&&'function'==typeof Symbol&&e.constructor===Symbol&&e!==Symbol.prototype?'symbol':typeof e},t=function(e,t){if(!(e instanceof t))throw new TypeError('Cannot call a class as a function')},n=function(){function e(e,t){for(var n=0;n<t.length;n++){var r=t[n];r.enumerable=r.enumerable||!1,r.configurable=!0,'value'in r&&(r.writable=!0),Object.defineProperty(e,r.key,r)}}return function(t,n,r){return n&&e(t.prototype,n),r&&e(t,r),t}}(),r=Object.assign||function(e){for(var t=1;t<arguments.length;t++){var n=arguments[t];for(var r in n)Object.prototype.hasOwnProperty.call(n,r)&&(e[r]=n[r])}return e},i=function(){function e(n){var r=!(arguments.length>1&&void 0!==arguments[1])||arguments[1],i=arguments.length>2&&void 0!==arguments[2]?arguments[2]:[],o=arguments.length>3&&void 0!==arguments[3]?arguments[3]:5e3;t(this,e),this.ctx=n,this.iframes=r,this.exclude=i,this.iframesTimeout=o}return n(e,[{key:'getContexts',value:function(){var e=[];return(void 0!==this.ctx&&this.ctx?NodeList.prototype.isPrototypeOf(this.ctx)?Array.prototype.slice.call(this.ctx):Array.isArray(this.ctx)?this.ctx:'string'==typeof this.ctx?Array.prototype.slice.call(document.querySelectorAll(this.ctx)):[this.ctx]:[]).forEach(function(t){var n=e.filter(function(e){return e.contains(t)}).length>0;-1!==e.indexOf(t)||n||e.push(t)}),e}},{key:'getIframeContents',value:function(e,t){var n=arguments.length>2&&void 0!==arguments[2]?arguments[2]:function(){},r=void 0;try{var i=e.contentWindow;if(r=i.document,!i||!r)throw new Error('iframe inaccessible')}catch(e){n()}r&&t(r)}},{key:'isIframeBlank',value:function(e){var t=e.getAttribute('src').trim();return'about:blank'===e.contentWindow.location.href&&'about:blank'!==t&&t}},{key:'observeIframeLoad',value:function(e,t,n){var r=this,i=!1,o=null,a=function a(){if(!i){i=!0,clearTimeout(o);try{r.isIframeBlank(e)||(e.removeEventListener('load',a),r.getIframeContents(e,t,n))}catch(e){n()}}};e.addEventListener('load',a),o=setTimeout(a,this.iframesTimeout)}},{key:'onIframeReady',value:function(e,t,n){try{'complete'===e.contentWindow.document.readyState?this.isIframeBlank(e)?this.observeIframeLoad(e,t,n):this.getIframeContents(e,t,n):this.observeIframeLoad(e,t,n)}catch(e){n()}}},{key:'waitForIframes',value:function(e,t){var n=this,r=0;this.forEachIframe(e,function(){return!0},function(e){r++,n.waitForIframes(e.querySelector('html'),function(){--r||t()})},function(e){e||t()})}},{key:'forEachIframe',value:function(t,n,r){var i=this,o=arguments.length>3&&void 0!==arguments[3]?arguments[3]:function(){},a=t.querySelectorAll('iframe'),s=a.length,c=0;a=Array.prototype.slice.call(a);var u=function(){--s<=0&&o(c)};s||u(),a.forEach(function(t){e.matches(t,i.exclude)?u():i.onIframeReady(t,function(e){n(t)&&(c++,r(e)),u()},u)})}},{key:'createIterator',value:function(e,t,n){return document.createNodeIterator(e,t,n,!1)}},{key:'createInstanceOnIframe',value:function(t){return new e(t.querySelector('html'),this.iframes)}},{key:'compareNodeIframe',value:function(e,t,n){if(e.compareDocumentPosition(n)&Node.DOCUMENT_POSITION_PRECEDING){if(null===t)return!0;if(t.compareDocumentPosition(n)&Node.DOCUMENT_POSITION_FOLLOWING)return!0}return!1}},{key:'getIteratorNode',value:function(e){var t=e.previousNode();return{prevNode:t,node:null===t?e.nextNode():e.nextNode()&&e.nextNode()}}},{key:'checkIframeFilter',value:function(e,t,n,r){var i=!1,o=!1;return r.forEach(function(e,t){e.val===n&&(i=t,o=e.handled)}),this.compareNodeIframe(e,t,n)?(!1!==i||o?!1===i||o||(r[i].handled=!0):r.push({val:n,handled:!0}),!0):(!1===i&&r.push({val:n,handled:!1}),!1)}},{key:'handleOpenIframes',value:function(e,t,n,r){var i=this;e.forEach(function(e){e.handled||i.getIframeContents(e.val,function(e){i.createInstanceOnIframe(e).forEachNode(t,n,r)})})}},{key:'iterateThroughNodes',value:function(e,t,n,r,i){for(var o,a=this,s=this.createIterator(t,e,r),c=[],u=[],l=void 0,h=void 0;void 0,o=a.getIteratorNode(s),h=o.prevNode,l=o.node;)this.iframes&&this.forEachIframe(t,function(e){return a.checkIframeFilter(l,h,e,c)},function(t){a.createInstanceOnIframe(t).forEachNode(e,function(e){return u.push(e)},r)}),u.push(l);u.forEach(function(e){n(e)}),this.iframes&&this.handleOpenIframes(c,e,n,r),i()}},{key:'forEachNode',value:function(e,t,n){var r=this,i=arguments.length>3&&void 0!==arguments[3]?arguments[3]:function(){},o=this.getContexts(),a=o.length;a||i(),o.forEach(function(o){var s=function(){r.iterateThroughNodes(e,o,t,n,function(){--a<=0&&i()})};r.iframes?r.waitForIframes(o,s):s()})}}],[{key:'matches',value:function(e,t){var n='string'==typeof t?[t]:t,r=e.matches||e.matchesSelector||e.msMatchesSelector||e.mozMatchesSelector||e.oMatchesSelector||e.webkitMatchesSelector;if(r){var i=!1;return n.every(function(t){return!r.call(e,t)||(i=!0,!1)}),i}return!1}}]),e}(),o=function(){function o(e){t(this,o),this.ctx=e,this.ie=!1;var n=window.navigator.userAgent;(n.indexOf('MSIE')>-1||n.indexOf('Trident')>-1)&&(this.ie=!0)}return n(o,[{key:'log',value:function(t){var n=arguments.length>1&&void 0!==arguments[1]?arguments[1]:'debug',r=this.opt.log;this.opt.debug&&'object'===(void 0===r?'undefined':e(r))&&'function'==typeof r[n]&&r[n]('mark.js: '+t)}},{key:'escapeStr',value:function(e){return e.replace(/[\\-\\[\\]\\/\\{\\}\\(\\)\\*\\+\\?\\.\\\\\\^\\$\\|]/g,'\\\\$&')}},{key:'createRegExp',value:function(e){return'disabled'!==this.opt.wildcards&&(e=this.setupWildcardsRegExp(e)),e=this.escapeStr(e),Object.keys(this.opt.synonyms).length&&(e=this.createSynonymsRegExp(e)),(this.opt.ignoreJoiners||this.opt.ignorePunctuation.length)&&(e=this.setupIgnoreJoinersRegExp(e)),this.opt.diacritics&&(e=this.createDiacriticsRegExp(e)),e=this.createMergedBlanksRegExp(e),(this.opt.ignoreJoiners||this.opt.ignorePunctuation.length)&&(e=this.createJoinersRegExp(e)),'disabled'!==this.opt.wildcards&&(e=this.createWildcardsRegExp(e)),e=this.createAccuracyRegExp(e)}},{key:'createSynonymsRegExp',value:function(e){var t=this.opt.synonyms,n=this.opt.caseSensitive?'':'i',r=this.opt.ignoreJoiners||this.opt.ignorePunctuation.length?'\\0':'';for(var i in t)if(t.hasOwnProperty(i)){var o=t[i],a='disabled'!==this.opt.wildcards?this.setupWildcardsRegExp(i):this.escapeStr(i),s='disabled'!==this.opt.wildcards?this.setupWildcardsRegExp(o):this.escapeStr(o);''!==a&&''!==s&&(e=e.replace(new RegExp('('+this.escapeStr(a)+'|'+this.escapeStr(s)+')','gm'+n),r+'('+this.processSynomyms(a)+'|'+this.processSynomyms(s)+')'+r))}return e}},{key:'processSynomyms',value:function(e){return(this.opt.ignoreJoiners||this.opt.ignorePunctuation.length)&&(e=this.setupIgnoreJoinersRegExp(e)),e}},{key:'setupWildcardsRegExp',value:function(e){return(e=e.replace(/(?:\\\\)*\\?/g,function(e){return'\\\\'===e.charAt(0)?'?':''})).replace(/(?:\\\\)*\\*/g,function(e){return'\\\\'===e.charAt(0)?'*':''})}},{key:'createWildcardsRegExp',value:function(e){var t='withSpaces'===this.opt.wildcards;return e.replace(/\\u0001/g,t?'[\\\\S\\\\s]?':'\\\\S?').replace(/\\u0002/g,t?'[\\\\S\\\\s]*?':'\\\\S*')}},{key:'setupIgnoreJoinersRegExp',value:function(e){return e.replace(/[^(|)\\\\]/g,function(e,t,n){var r=n.charAt(t+1);return/[(|)\\\\]/.test(r)||''===r?e:e+'\\0'})}},{key:'createJoinersRegExp',value:function(e){var t=[],n=this.opt.ignorePunctuation;return Array.isArray(n)&&n.length&&t.push(this.escapeStr(n.join(''))),this.opt.ignoreJoiners&&t.push('\\\\u00ad\\\\u200b\\\\u200c\\\\u200d'),t.length?e.split(/\\u0000+/).join('['+t.join('')+']*'):e}},{key:'createDiacriticsRegExp',value:function(e){var t=this.opt.caseSensitive?'':'i',n=this.opt.caseSensitive?['aàáảãạăằắẳẵặâầấẩẫậäåāą','AÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÄÅĀĄ','cçćč','CÇĆČ','dđď','DĐĎ','eèéẻẽẹêềếểễệëěēę','EÈÉẺẼẸÊỀẾỂỄỆËĚĒĘ','iìíỉĩịîïī','IÌÍỈĨỊÎÏĪ','lł','LŁ','nñňń','NÑŇŃ','oòóỏõọôồốổỗộơởỡớờợöøō','OÒÓỎÕỌÔỒỐỔỖỘƠỞỠỚỜỢÖØŌ','rř','RŘ','sšśșş','SŠŚȘŞ','tťțţ','TŤȚŢ','uùúủũụưừứửữựûüůū','UÙÚỦŨỤƯỪỨỬỮỰÛÜŮŪ','yýỳỷỹỵÿ','YÝỲỶỸỴŸ','zžżź','ZŽŻŹ']:['aàáảãạăằắẳẵặâầấẩẫậäåāąAÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬÄÅĀĄ','cçćčCÇĆČ','dđďDĐĎ','eèéẻẽẹêềếểễệëěēęEÈÉẺẼẸÊỀẾỂỄỆËĚĒĘ','iìíỉĩịîïīIÌÍỈĨỊÎÏĪ','lłLŁ','nñňńNÑŇŃ','oòóỏõọôồốổỗộơởỡớờợöøōOÒÓỎÕỌÔỒỐỔỖỘƠỞỠỚỜỢÖØŌ','rřRŘ','sšśșşSŠŚȘŞ','tťțţTŤȚŢ','uùúủũụưừứửữựûüůūUÙÚỦŨỤƯỪỨỬỮỰÛÜŮŪ','yýỳỷỹỵÿYÝỲỶỸỴŸ','zžżźZŽŻŹ'],r=[];return e.split('').forEach(function(i){n.every(function(n){if(-1!==n.indexOf(i)){if(r.indexOf(n)>-1)return!1;e=e.replace(new RegExp('['+n+']','gm'+t),'['+n+']'),r.push(n)}return!0})}),e}},{key:'createMergedBlanksRegExp',value:function(e){return e.replace(/[\\s]+/gim,'[\\\\s]+')}},{key:'createAccuracyRegExp',value:function(e){var t=this,n=this.opt.accuracy,r='string'==typeof n?n:n.value,i='';switch(('string'==typeof n?[]:n.limiters).forEach(function(e){i+='|'+t.escapeStr(e)}),r){case'partially':default:return'()('+e+')';case'complementary':return'()([^'+(i='\\\\s'+(i||this.escapeStr('!\\'#$%&'()*+,-./:;<=>?@[\\\\]^_`{|}~¡¿')))+']*'+e+'[^'+i+']*)';case'exactly':return'(^|\\\\s'+i+')('+e+')(?=$|\\\\s'+i+')'}}},{key:'getSeparatedKeywords',value:function(e){var t=this,n=[];return e.forEach(function(e){t.opt.separateWordSearch?e.split(' ').forEach(function(e){e.trim()&&-1===n.indexOf(e)&&n.push(e)}):e.trim()&&-1===n.indexOf(e)&&n.push(e)}),{keywords:n.sort(function(e,t){return t.length-e.length}),length:n.length}}},{key:'isNumeric',value:function(e){return Number(parseFloat(e))==e}},{key:'checkRanges',value:function(e){var t=this;if(!Array.isArray(e)||'[object Object]'!==Object.prototype.toString.call(e[0]))return this.log('markRanges() will only accept an array of objects'),this.opt.noMatch(e),[];var n=[],r=0;return e.sort(function(e,t){return e.start-t.start}).forEach(function(e){var i=t.callNoMatchOnInvalidRanges(e,r),o=i.start,a=i.end;i.valid&&(e.start=o,e.length=a-o,n.push(e),r=a)}),n}},{key:'callNoMatchOnInvalidRanges',"+
            "value:function(e,t){var n=void 0,r=void 0,i=!1;return e&&void 0!==e.start?(r=(n=parseInt(e.start,10))+parseInt(e.length,10),this.isNumeric(e.start)&&this.isNumeric(e.length)&&r-t>0&&r-n>0?i=!0:(this.log('Ignoring invalid or overlapping range: '+JSON.stringify(e)),this.opt.noMatch(e))):(this.log('Ignoring invalid range: '+JSON.stringify(e)),this.opt.noMatch(e)),{start:n,end:r,valid:i}}},{key:'checkWhitespaceRanges',value:function(e,t,n){var r=void 0,i=!0,o=n.length,a=t-o,s=parseInt(e.start,10)-a;return(r=(s=s>o?o:s)+parseInt(e.length,10))>o&&(r=o,this.log('End range automatically set to the max value of '+o)),s<0||r-s<0||s>o||r>o?(i=!1,this.log('Invalid range: '+JSON.stringify(e)),this.opt.noMatch(e)):''===n.substring(s,r).replace(/\\s+/g,'')&&(i=!1,this.log('Skipping whitespace only range: '+JSON.stringify(e)),this.opt.noMatch(e)),{start:s,end:r,valid:i}}},{key:'getTextNodes',value:function(e){var t=this,n='',r=[];this.iterator.forEachNode(NodeFilter.SHOW_TEXT,function(e){r.push({start:n.length,end:(n+=e.textContent).length,node:e})},function(e){return t.matchesExclude(e.parentNode)?NodeFilter.FILTER_REJECT:NodeFilter.FILTER_ACCEPT},function(){e({value:n,nodes:r})})}},{key:'matchesExclude',value:function(e){return i.matches(e,this.opt.exclude.concat(['script','style','title','head','html']))}},{key:'wrapRangeInTextNode',value:function(e,t,n){var r=this.opt.element?this.opt.element:'mark',i=e.splitText(t),o=i.splitText(n-t),a=document.createElement(r);return a.setAttribute('data-markjs','true'),this.opt.className&&a.setAttribute('class',this.opt.className),a.textContent=i.textContent,i.parentNode.replaceChild(a,i),o}},{key:'wrapRangeInMappedTextNode',value:function(e,t,n,r,i){var o=this;e.nodes.every(function(a,s){var c=e.nodes[s+1];if(void 0===c||c.start>t){if(!r(a.node))return!1;var u=t-a.start,l=(n>a.end?a.end:n)-a.start,h=e.value.substr(0,a.start),f=e.value.substr(l+a.start);if(a.node=o.wrapRangeInTextNode(a.node,u,l),e.value=h+f,e.nodes.forEach(function(t,n){n>=s&&(e.nodes[n].start>0&&n!==s&&(e.nodes[n].start-=l),e.nodes[n].end-=l)}),n-=l,i(a.node.previousSibling,a.start),!(n>a.end))return!1;t=a.end}return!0})}},{key:'wrapMatches',value:function(e,t,n,r,i){var o=this,a=0===t?0:t+1;this.getTextNodes(function(t){t.nodes.forEach(function(t){t=t.node;for(var i=void 0;null!==(i=e.exec(t.textContent))&&''!==i[a];)if(n(i[a],t)){var s=i.index;if(0!==a)for(var c=1;c<a;c++)s+=i[c].length;t=o.wrapRangeInTextNode(t,s,s+i[a].length),r(t.previousSibling),e.lastIndex=0}}),i()})}},{key:'wrapMatchesAcrossElements',value:function(e,t,n,r,i){var o=this,a=0===t?0:t+1;this.getTextNodes(function(t){for(var s=void 0;null!==(s=e.exec(t.value))&&''!==s[a];){var c=s.index;if(0!==a)for(var u=1;u<a;u++)c+=s[u].length;var l=c+s[a].length;o.wrapRangeInMappedTextNode(t,c,l,function(e){return n(s[a],e)},function(t,n){e.lastIndex=n,r(t)})}i()})}},{key:'wrapRangeFromIndex',value:function(e,t,n,r){var i=this;this.getTextNodes(function(o){var a=o.value.length;e.forEach(function(e,r){var s=i.checkWhitespaceRanges(e,a,o.value),c=s.start,u=s.end;s.valid&&i.wrapRangeInMappedTextNode(o,c,u,function(n){return t(n,e,o.value.substring(c,u),r)},function(t){n(t,e)})}),r()})}},{key:'unwrapMatches',value:function(e){for(var t=e.parentNode,n=document.createDocumentFragment();e.firstChild;)n.appendChild(e.removeChild(e.firstChild));t.replaceChild(n,e),this.ie?this.normalizeTextNode(t):t.normalize()}},{key:'normalizeTextNode',value:function(e){if(e){if(3===e.nodeType)for(;e.nextSibling&&3===e.nextSibling.nodeType;)e.nodeValue+=e.nextSibling.nodeValue,e.parentNode.removeChild(e.nextSibling);else this.normalizeTextNode(e.firstChild);this.normalizeTextNode(e.nextSibling)}}},{key:'markRegExp',value:function(e,t){var n=this;this.opt=t,this.log('Searching with expression ''+e+''');var r=0,i='wrapMatches';this.opt.acrossElements&&(i='wrapMatchesAcrossElements'),this[i](e,this.opt.ignoreGroups,function(e,t){return n.opt.filter(t,e,r)},function(e){r++,n.opt.each(e)},function(){0===r&&n.opt.noMatch(e),n.opt.done(r)})}},{key:'mark',value:function(e,t){var n=this;this.opt=t;var r=0,i='wrapMatches',o=this.getSeparatedKeywords('string'==typeof e?[e]:e),a=o.keywords,s=o.length,c=this.opt.caseSensitive?'':'i';this.opt.acrossElements&&(i='wrapMatchesAcrossElements'),0===s?this.opt.done(r):function e(t){var o=new RegExp(n.createRegExp(t),'gm'+c),u=0;n.log('Searching with expression ''+o+'''),n[i](o,1,function(e,i){return n.opt.filter(i,t,r,u)},function(e){u++,r++,n.opt.each(e)},function(){0===u&&n.opt.noMatch(t),a[s-1]===t?n.opt.done(r):e(a[a.indexOf(t)+1])})}(a[0])}},{key:'markRanges',value:function(e,t){var n=this;this.opt=t;var r=0,i=this.checkRanges(e);i&&i.length?(this.log('Starting to mark with the following ranges: '+JSON.stringify(i)),this.wrapRangeFromIndex(i,function(e,t,r,i){return n.opt.filter(e,t,r,i)},function(e,t){r++,n.opt.each(e,t)},function(){n.opt.done(r)})):this.opt.done(r)}},{key:'unmark',value:function(e){var t=this;this.opt=e;var n=this.opt.element?this.opt.element:'*';n+='[data-markjs]',this.opt.className&&(n+='.'+this.opt.className),this.log('Removal selector ''+n+'''),this.iterator.forEachNode(NodeFilter.SHOW_ELEMENT,function(e){t.unwrapMatches(e)},function(e){var r=i.matches(e,n),o=t.matchesExclude(e);return!r||o?NodeFilter.FILTER_REJECT:NodeFilter.FILTER_ACCEPT},this.opt.done)}},{key:'opt',set:function(e){this._opt=r({},{element:'',className:'',exclude:[],iframes:!1,iframesTimeout:5e3,separateWordSearch:!0,diacritics:!0,synonyms:{},accuracy:'partially',acrossElements:!1,caseSensitive:!1,ignoreJoiners:!1,ignoreGroups:0,ignorePunctuation:[],wildcards:'disabled',each:function(){},noMatch:function(){},filter:function(){return!0},done:function(){},debug:!1,log:window.console},e)},get:function(){return this._opt}},{key:'iterator',get:function(){return new i(this.ctx,this.opt.iframes,this.opt.exclude,this.opt.iframesTimeout)}}]),o}();return function(e){var t=this,n=new o(e);return this.mark=function(e,r){return n.mark(e,r),t},this.markRegExp=function(e,r){return n.markRegExp(e,r),t},this.markRanges=function(e,r){return n.markRanges(e,r),t},this.unmark=function(e){return n.unmark(e),t},this}});" +
            "window.webview_native_match_marker = new Marker(document.body);};"
            ,"native/mark.js", 0);
        }
    }

    public void findAllAsync(String find){
        findAll(find);
    }

    public void findNext(boolean forward){
        //todo
    }

    public void clearSslPreferences(){
        //todo
    }

    public void clearView(){
        mBrowser.loadURL("about:blank");
    }

    public void computeScroll(){
        
    }

    public WebBackForwardList copyBackForwardList(){
        return mForwardBackwardList.clone();
    }

    public PrintDocumentAdapter createPrintDocumentAdapter(String documentName){

        String tmpdr = System.getProperty("java.io.tmpdr");
        

        String b = "TEMP_PDF_WEBVIEW.pdf";
        try {
            RandomGenerator rand = RandomGenerator.of("Random");
            b = Double.toHexString(rand.nextDouble(0.0, 9999999.9999999));
            Files.createFile(Path.of(tmpdr, b));
        } catch (Exception e) {
            //TODO: handle exception
        }
        
        String p = Path.of(tmpdr, b).toString();
        mBrowser.printToPDF(p, new CefPdfPrintSettings(), new CefPdfPrintCallback() {
            @Override
            public void onPdfPrintFinished(String path, boolean ok) {
                
            }
        });

        try{
            return new PrintFSDocumentAdapter(p);
        } catch(Exception e){
            return null;
        }
    }

    private boolean mIs_msg_chan_created;
    private WebMessagePort mWebMessagePort1;
    private WebMessagePort mWebMessagePort2;

    private WebMessagePort.WebMessageCallback mWebMessageCallback1;
    private WebMessagePort.WebMessageCallback mWebMessageCallback2;

    public WebMessagePort[] createWebMessageChannel(){
        if (!mIs_msg_chan_created){
            
            mWebMessagePort1 = new WebMessagePort() {
                @Override
                public void postMessage(WebMessage message) {
                    mBrowser.executeJavaScript("window.webview_WebMessageChannel.port1.postMessage(`"+message.getData()+"`);", "webview/postMessage", 0);
                }
                @Override
                public void close() {
                    
                }

                @Override
                void setWebMessageCallback(WebMessageCallback callback) {
                    mWebMessageCallback1 = callback;
                }

                @Override
                public void setWebMessageCallback(WebMessageCallback callback, Handler handler) {
                    mWebMessageCallback1 = callback;
                }
            };
            mWebMessagePort2 = new WebMessagePort() {
                @Override
                public void postMessage(WebMessage message) {
                    mBrowser.executeJavaScript("window.webview_WebMessageChannel.port2.postMessage(`"+message.getData()+"`);", "webview/postMessage", 0);
                }
                @Override
                public void close() {
                    
                }

                @Override
                void setWebMessageCallback(WebMessageCallback callback) {
                    mWebMessageCallback2 = callback;
                }

                @Override
                public void setWebMessageCallback(WebMessageCallback callback, Handler handler) {
                    mWebMessageCallback2 = callback;
                }
            };

            CefMessageRouterConfig config = new CefMessageRouterConfig();
            config.jsQueryFunction = "cefQuery";
            config.jsCancelFunction = "cefWueryCancel";

            CefMessageRouter router = CefMessageRouter.create(config);

            router.addHandler(new CefMessageRouterHandler(){
                private Map<String, Long> mNativeRef;

                @Override
                public boolean onQuery(CefBrowser arg0, CefFrame arg1, long queryId, String request, boolean persistent,
                        CefQueryCallback callback) 
                {
                    if (request.startsWith("1:") && mWebMessageCallback1 !=null){
                        mWebMessageCallback1.onMessage(mWebMessagePort1, new WebMessage(request.substring(2)));
                    }
                    if (request.startsWith("2:") && mWebMessageCallback2 !=null){
                        mWebMessageCallback2.onMessage(mWebMessagePort2, new WebMessage(request.substring(2)));
                    }
                    return false;
                }
                @Override
                public void onQueryCanceled(CefBrowser arg0, CefFrame arg1, long arg2) {
                    // TODO Auto-generated method stub
                    
                }

                @Override
                public long getNativeRef(String key) {
                    // TODO Auto-generated method stub
                    Long l = mNativeRef.get(key);
                    if (l==null){
                        return 0;
                    } else{
                        return l.longValue();
                    }
                }

                @Override
                public void setNativeRef(String key, long value) {
                    if (mNativeRef == null){
                        mNativeRef = new HashMap<String, Long>();
                    };
                    mNativeRef.put(key, Long.valueOf(value));
                }
                
            }, true);

            mBrowser.getClient().addMessageRouter(router);
            
            mBrowser.executeJavaScript(
                "{window.webview_WebMessageChannel= new MessageChannel();window.webview_WebMessageChannel.port2.addEventListener('message',(e)=>{window.cefQuery({request:'2:'+e.data.toString(),persistent:false})});window.webview_WebMessageChannel.port1.addEventListener('message',(e)=>{window.cefQuery({request:'1:'+e.data.toString(),persistent:false})})}",
                "webview/createWebMessageChannel", 0);

            
        };
        mIs_msg_chan_created = true;

        return new WebMessagePort[]{mWebMessagePort1, mWebMessagePort2};
    }

    public void destroy(){
        mBrowser.getClient().dispose();
    }

    public void disableWebView(){
        //todo
    }

    /* api level 31 todo
    public void dispatchCreateViewTranslationRequest(Map<AutofillId, long[]> viewIds, int[] supportedFormats, TranslationCapability capability, List<ViewTranslationRequest> requests){

    }
    */

    public boolean dispatchKeyEvent(){
        
    }
}
