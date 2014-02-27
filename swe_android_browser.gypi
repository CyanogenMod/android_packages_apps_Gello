{
  'targets' : [
    {
      'target_name': 'swe_android_browser_apk',
      'type': 'none',
      'dependencies': [
        'swe_engine_java',
        'android-support-v13',
        '<@(libnetxt_dependencies)',
      ],
      'variables': {
        'apk_name': 'SWE_AndroidBrowser',
        'manifest_package_name': 'com.android.swe.browser',
        'app_manifest_version_name': '<!(../swe/browser/tools/generate_about.sh --quiet --name --about)',
        'app_manifest_version_code': '<!(../swe/browser/tools/generate_about.sh --quiet --code)',
        'java_in_dir': '.',
        'resource_dir': '../../swe/browser/res',
        'native_lib_target': 'libswewebviewchromium',
        'additional_input_paths': ['<(PRODUCT_DIR)/android_webview_apk/assets/webviewchromium.pak'],
        'additional_native_libs': ['<@(libnetxt_native_libs)']
      },
      'copies': [
        {
              'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/assets/',
              'files': [
                '<(PRODUCT_DIR)/android_webview_apk/assets/webviewchromium.pak'
              ],
        },
      ],
      'includes': [ '../../build/java_apk.gypi' ],
    },
  ],
}
