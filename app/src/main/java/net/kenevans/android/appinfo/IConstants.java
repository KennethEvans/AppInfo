//Copyright (c) 2011 Kenneth Evans
//
//Permission is hereby granted, free of charge, to any person obtaining
//a copy of this software and associated documentation files (the
//"Software"), to deal in the Software without restriction, including
//without limitation the rights to use, copy, modify, merge, publish,
//distribute, sublicense, and/or sell copies of the Software, and to
//permit persons to whom the Software is furnished to do so, subject to
//the following conditions:
//
//The above copyright notice and this permission notice shall be included
//in all copies or substantial portions of the Software.
//
//THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
//EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
//MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
//IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
//CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
//TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
//SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package net.kenevans.android.appinfo;

import android.net.Uri;

/**
 * Holds constant values used by several classes in the application.
 */
public interface IConstants {
    // Log tag
    /**
     * Tag to associate with log messages.
     */
    public static final String TAG = "AppInfo";

    // Information
    /**
     * Key for information URL sent to InfoActivity.
     */
    public static final String INFO_URL = "InformationURL";

    /** KB/byte. Converts bytes to KB. */
    public static final double KB = 1. / 1024.;
    /** MB/byte. Converts bytes to MB. */
    public static final double MB = 1. / (1024. * 1024.);
    /** GB/byte. Converts bytes to GB. */
    public static final double GB = 1. / (1024. * 1024. * 1024.);

}
