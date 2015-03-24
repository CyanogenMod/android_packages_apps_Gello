{
  'targets' : [
    {
      'target_name': 'swe_android_browser_apk',
      'type': 'none',
      'dependencies': [
        'swe_chrome_engine',
        #'android-support-v13',
      ],
      'variables': {
        'apk_name': 'SWE_AndroidBrowser',
        'manifest_package_name': 'com.android.browser',
        'native_lib_version_name': '<(version_full)',
        #'package_name': 'swe_android_browser_apk',
        'java_in_dir': '.',
        'resource_dir': '../browser/res',
        'assets_dir': '../../swe/browser/assets',
        'conditions': [
          ['icu_use_data_file_flag==1', {
            'additional_input_paths': [
              '<(PRODUCT_DIR)/icudtl.dat',
            ],
          }],
        ],
        'native_lib_target': 'libswe',
        'app_manifest_version_name': '<!(../swe/browser/tools/generate_about.sh --quiet --name --about)',
        'app_manifest_version_code': '<!(../swe/browser/tools/generate_about.sh --quiet --code)',
        #'additional_native_libs': [
        #  '<@(libnetxt_native_libs)',
        #  ],
        'additional_input_paths': [
          '<@(chrome_android_pak_output_resources)',
        ],
        'override_package_name': 'org.codeaurora.swe.browser.beta',
        'android_manifest_path': '../../swe/browser/AndroidManifest.xml',
        'additional_src_dirs': ['<(DEPTH)/swe/browser/src_standalone/com/android/browser'],
      },

      'copies': [
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/assets/',
          'files': [
            '<@(chrome_android_pak_input_resources)',
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
