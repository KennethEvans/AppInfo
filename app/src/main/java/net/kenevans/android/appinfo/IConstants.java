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

/**
 * Holds constant values used by several classes in the application.
 */
interface IConstants {
    /**
     * Tag to associate with log messages.
     */
    String TAG = "AppInfo";

    /**
     * Result code for creating a document.
     */
    int CREATE_DOCUMENT = 10;

    // Preferences
    String PREF_FILTER = "filter";
    String PREF_DO_BUILD = "doBuildInfo";
    String PREF_DO_MEMORY = "doMemoryInfo";
    String PREF_DO_PREFERRED_APPLICATIONS = "doPreferredApplications";
    String PREF_DO_NON_SYSTEM = "doNonSystemApps";
    String PREF_DO_SYSTEM = "doSystemApps";
    String PREF_DO_PERMISSIONS = "doPermissions";

    // Information
    /**
     * Key for information URL sent to InfoActivity.
     */
    String INFO_URL = "InformationURL";

    /** KB/byte. Converts bytes to KB. */
    double KB = 1. / 1024.;
    /** MB/byte. Converts bytes to MB. */
    double MB = 1. / (1024. * 1024.);
    /** GB/byte. Converts bytes to GB. */
    double GB = 1. / (1024. * 1024. * 1024.);

}
