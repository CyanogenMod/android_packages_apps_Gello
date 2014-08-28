{
  'targets' : [
    {
      'target_name': 'swe_android_browser_apk',
      'type': 'none',
      'dependencies': [
        'swe_engine_java',
        'android-support-v13',
      ],
      'variables': {
        'apk_name': 'SWE_AndroidBrowser',
        'manifest_package_name': 'com.android.browser',
        'app_manifest_version_name': '<!(../swe/browser/tools/generate_about.sh --quiet --name --about)',
        'app_manifest_version_code': '<!(../swe/browser/tools/generate_about.sh --quiet --code)',
        'java_in_dir': '.',
        'resource_dir': '../../swe/browser/res',
        'assets_dir': '../../swe/browser/assets',
        'native_lib_target': 'libswewebviewchromium',
        'additional_input_paths': ['<(PRODUCT_DIR)/android_webview_apk/assets/webviewchromium.pak'],
        'conditions': [
          ['icu_use_data_file_flag==1', {
            'additional_input_paths': [
              '<(PRODUCT_DIR)/icudtl.dat',
            ],
          }],
        ],
        'override_package_name': 'com.android.swe.browser',
        'android_manifest_path': '../../swe/browser/AndroidManifest.xml',
        'additional_src_dirs': ['<(DEPTH)/swe/browser/src_standalone/com/android/browser'],
      },
      'copies': [
        {
              'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/assets/',
              'files': [
                '<(PRODUCT_DIR)/android_webview_apk/assets/webviewchromium.pak'
              ],
              'conditions': [
                ['icu_use_data_file_flag==1', {
                  'files': [
                    '<(PRODUCT_DIR)/icudtl.dat',
                ],
                }],
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
    {
      'target_name': 'swe_android_browser_apk_java',
      'type': 'none',
      'dependencies': [
        'swe_android_browser_apk',
      ],
      'includes': [ '../../build/apk_fake_jar.gypi' ],
    },
  ],
}
