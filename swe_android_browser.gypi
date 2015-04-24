{
  'variables' : {
    'manifest_package_name%' : 'org.codeaurora.swe.browser.beta',
    'manifest_test_package_name%' : 'org.codeaurora.swe.browser.beta.tests',
  },
  'targets' : [
    {
      'target_name': 'swe_android_browser_apk',
      'type': 'none',
      'dependencies': [
        'swe_chrome_engine_java',
        '<@(libnetxt_dependencies)',
        '<@(libsta_dependencies)',
        '<@(libsweadrenoext_dependencies)',
        '<@(web_refiner_dependencies)',
        'fast_webview_java',
        #'android-support-v13',
      ],
      'variables': {
        'apk_name': 'SWE_AndroidBrowser',
        'native_lib_version_name': '<(version_full)',
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
        'app_manifest_version_code': '<!(python <(DEPTH)/swe/tools/swe_version.py \
                                     -i <(DEPTH)/chrome/VERSION \
                                     -o <(DEPTH)/swe/VERSION --version-code-only)',
        'app_manifest_version_name': '<!(python <(DEPTH)/swe/tools/swe_version.py \
                                     -i <(DEPTH)/chrome/VERSION \
                                     -o <(DEPTH)/swe/VERSION --version-string-only)',
        'generate_about_string': '<!(python <(DEPTH)/swe/tools/swe_version.py \
                                     -i <(DEPTH)/chrome/VERSION \
                                     -o <(DEPTH)/swe/VERSION \
                                     -about <(DEPTH)/swe/browser/res/values/about.xml)',
        'additional_native_libs': [
          '<@(libnetxt_native_libs)',
          '<@(libsta_native_libs)',
          '<@(libsweadrenoext_native_libs)',
          '<@(web_refiner_native_libs)',
        ],
        'additional_input_paths': [
          '<@(chrome_android_pak_output_resources)',
        ],
        'override_package_name': '<(manifest_package_name)',
        'android_manifest_path': '<(SHARED_INTERMEDIATE_DIR)/swe_android_browser_apk/AndroidManifest.xml',
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
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/assets/web_refiner',
          'files': [
            '<(assets_dir)/web_refiner/web_refiner.conf',
          ],
        },
      ],
      'includes': [ '../../build/java_apk.gypi' ],
    },
    {
      'target_name': 'swe_android_browser_apk_manifest',
      'type': 'none',
      'variables': {
        'jinja_inputs': ['<(DEPTH)/swe/browser/AndroidManifest.xml',
                        ],
        'jinja_output': '<(SHARED_INTERMEDIATE_DIR)/swe_android_browser_apk/AndroidManifest.xml',
        'standalone_manifest_package_name' : 'org.codeaurora.swe.browser.beta',
        'jinja_variables': ['package_name=<(standalone_manifest_package_name)',
                            'apk_label=@string/application_name_swe',
                            'apk_icon=@mipmap/ic_launcher_browser_swe_beta',
                            'apk_task_affinity=<(standalone_manifest_package_name)',
                            'apk_authorities=swe.browser.beta',],
      },
      'includes': [ '../../build/android/jinja_template.gypi' ],
    },
    {
      'target_name': 'swe_android_browser_fake_apk',
      'type': 'none',
      'dependencies': [
        'swe_android_browser_apk',
      ],
      'includes': [ '../../build/apk_fake_jar.gypi' ],
    },

    {
      'target_name': 'swe_android_browser_tests_apk',
        'type': 'none',
        'dependencies': [
          'swe_android_browser_fake_apk',
          '../base/base.gyp:base_java_test_support',
          '../content/content_shell_and_tests.gyp:content_java_test_support',
          '../net/net.gyp:net_java_test_support',
        ],
        'variables': {
          'apk_name': 'SWEBrowserTests',
          'override_package_name': '<(manifest_test_package_name)',
          'android_manifest_path': './tests/AndroidManifest.xml',
          'java_in_dir': './tests/startup',
          'is_test_apk': 1,
          'test_suite_name': 'swe_android_browser_tests',
        },
        'includes': [ '../../build/java_apk.gypi' ],
    },
  ],
}
