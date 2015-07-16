{
  'variables' : {
    'browser_config_path': '<(DEPTH)/swe/browser/channels/<(swe_channel)/branding/BRANDING',
  },
  'targets' : [
    {
      'target_name': 'swe_android_browser_apk',
      'type': 'none',
      'dependencies': [
        'swe_android_browser_apk_config',
        '<@(swe_dependencies)',
        #add new dependencies in swe_common.gypi
      ],
      'variables': {
        'apk_name': 'SWE_AndroidBrowser',
        'native_lib_version_name': '<(version_full)',
        'java_in_dir': '.',
        'resource_dir': '../browser/res',
        'conditions': [
          ['icu_use_data_file_flag==1', {
            'additional_input_paths': [
              '<(PRODUCT_DIR)/icudtl.dat',
            ],
          }],
        ],
        'native_lib_target': 'libswe',
        'app_manifest_version_code': '<(swe_app_manifest_version_code)',
        'app_manifest_version_name': '<(swe_app_manifest_version_name)',

        'additional_native_libs': [
          '<@(swe_additional_native_libs)',
        ],
        'additional_input_paths': [
          '<@(chrome_android_pak_output_resources)',
        ],
        'res_extra_dirs': [ '<@(swe_extra_res_dirs)',
                          ],
        'override_package_name': '<!(python <(swe_py_config) \
                                     -i <(browser_config_path) \
                                     -c PACKAGE_NAME)',
        'android_manifest_path': '<(SHARED_INTERMEDIATE_DIR)/swe_android_browser_apk/AndroidManifest.xml',
      },

      'copies': [
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/assets/',
          'files': [
            '<@(swe_assets)',
          ],
        },
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/assets/wml',
          'files': [
            '<@(swe_assets_wml)',
          ],
        },
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/assets/web_refiner',
          'files': [
            '<@(swe_assets_webrefiner)',
          ],
        },
      ],
      'includes': [ '../../build/java_apk.gypi' ],
    },
    #generate AndroidManifest.xml
    {
      'target_name': 'swe_android_browser_apk_manifest',
      'type': 'none',
      'variables': {
        'manifest_input_path': '<(DEPTH)/swe/browser/AndroidManifest.xml.jinja2',
        'manifest_output_path': '<(SHARED_INTERMEDIATE_DIR)/swe_android_browser_apk/AndroidManifest.xml',
        'manifest_config_file_path': '<(browser_config_path)',
      },
      'includes': [ '../swe_generate_manifest.gypi' ],
    },

    {
      'target_name': 'swe_android_browser_apk_config',
      'type': 'none',
      'variables': {
        'template_input_path': '<(DEPTH)/swe/browser/template/com/android/browser/BrowserConfig.java.template',
        'template_output_path': '<(SHARED_INTERMEDIATE_DIR)/templates/<(_target_name)/com/android/browser/BrowserConfig.java',
        'template_config_file_path': '<(browser_config_path)',
      },
      'includes': [ '../swe_browser_config.gypi' ],
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
          'standalone_manifest_package_name': '<!(python <(swe_py_config) \
                                     -i <(browser_config_path) \
                                     -c PACKAGE_NAME)',
          'override_package_name': '<(standalone_manifest_package_name).tests',
          'android_manifest_path': './tests/AndroidManifest.xml',
          'java_in_dir': './tests/startup',
          'is_test_apk': 1,
          'test_suite_name': 'swe_android_browser_tests',
        },
        'includes': [ '../../build/java_apk.gypi' ],
    },
  ],
}
