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
        'apk_name': 'Browser',
        'manifest_package_name': 'com.android.browser',
        'app_manifest_version_name': '<!(../swe/browser/tools/generate_about.sh --quiet --name --about)',
        'app_manifest_version_code': '<!(../swe/browser/tools/generate_about.sh --quiet --code)',
        'java_in_dir': '.',
        'resource_dir': '../../swe/browser/res',
        'assets_dir': '../../swe/browser/assets',
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
        {
              'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/assets/wml',
              'files': [
                '<(assets_dir)/wml/swe_wml.xsl',
                '<(assets_dir)/wml/swe_wml.js',
                '<(assets_dir)/wml/swe_wml.css',
              ],
        },
      ],
      'includes': [ '../../build/java_apk.gypi' ],
    },
  ],
}
