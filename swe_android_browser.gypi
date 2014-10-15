{
  'targets' : [
    {
      'target_name': 'swe_android_browser_apk',
      'type': 'none',
      'dependencies': [
        'swe_engine_java',
        'swe_android_browser_paks',
        'android-support-v13',
        '<@(libnetxt_dependencies)',
        '<@(libsweadrenoext_dependencies)',
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
        'additional_native_libs': [
          '<@(libnetxt_native_libs)',
          '<@(libsweadrenoext_native_libs)'],
        'additional_input_paths': [
          '<!@pymod_do_main(swe_repack_locales -i -p <(OS) -g <(SHARED_INTERMEDIATE_DIR) -s <(SHARED_INTERMEDIATE_DIR) -x <(SHARED_INTERMEDIATE_DIR) <(locales))',
        ],
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
    {
      'target_name': 'swe_android_browser_paks',
      'type': 'none',
      'dependencies': [
        '<(DEPTH)/components/components_strings.gyp:components_strings',
        '<(DEPTH)/net/net.gyp:net_resources',
        '<(DEPTH)/ui/resources/ui_resources.gyp:ui_resources',
        '<(DEPTH)/ui/strings/ui_strings.gyp:ui_strings',
        '<(DEPTH)/content/app/strings/content_strings.gyp:content_strings',
        '<(DEPTH)/content/content_resources.gyp:content_resources',
        '<(DEPTH)/third_party/WebKit/public/blink_resources.gyp:blink_resources',
        '<(DEPTH)/webkit/glue/resources/webkit_resources.gyp:webkit_resources',
      ],
      'variables': {
        'repack_path': '<(DEPTH)/tools/grit/grit/format/repack.py',
      },
      'actions': [
        {
          'action_name': 'repack_non_locale_paks',
          'variables': {
            # pak_inputs should be in sync with pak_inputs in the
            # repack_android_webview_pack action within android_webview.gyp
            'pak_inputs': [
              '<(SHARED_INTERMEDIATE_DIR)/blink/public/resources/blink_resources.pak',
              '<(SHARED_INTERMEDIATE_DIR)/content/content_resources.pak',
              '<(SHARED_INTERMEDIATE_DIR)/net/net_resources.pak',
              '<(SHARED_INTERMEDIATE_DIR)/ui/resources/ui_resources_100_percent.pak',
              '<(SHARED_INTERMEDIATE_DIR)/webkit/webkit_resources_100_percent.pak',
              '<(SHARED_INTERMEDIATE_DIR)/components/component_resources.pak',
              '<(SHARED_INTERMEDIATE_DIR)/ui/resources/webui_resources.pak',
              '<(SHARED_INTERMEDIATE_DIR)/content/browser/tracing/tracing_resources.pak',
            ],
          },
          'inputs': [
            '<(repack_path)',
            '<@(pak_inputs)',
          ],
          'outputs': [
            '<(PRODUCT_DIR)/swe_android_browser_apk/assets/webviewchromium.pak',
          ],
          'action': ['python', '<(repack_path)', '<@(_outputs)',
                     '<@(pak_inputs)'],
        },
        {
          'action_name': 'repack_locale_paks',
          'variables': {
            'repack_extra_flags%': [],
            'repack_output_dir%': '<(PRODUCT_DIR)/swe_android_browser_apk/assets',
            'repack_locales_cmd': ['python', '<(DEPTH)/swe/tools/build/swe_repack_locales.py'],
            'grit_out_dir': '<(SHARED_INTERMEDIATE_DIR)/android_webview',
          },
          'inputs': [
            '../tools/build/swe_repack_locales.py',
            '<!@pymod_do_main(swe_repack_locales -i -p <(OS) -g <(grit_out_dir) -s <(SHARED_INTERMEDIATE_DIR) -x <(repack_output_dir) <(repack_extra_flags) <(locales))',
          ],
          'outputs': [
            '<!@pymod_do_main(swe_repack_locales -o -p <(OS) -g <(grit_out_dir) -s <(SHARED_INTERMEDIATE_DIR) -x <(repack_output_dir) <(locales))',
          ],
          'action': [
            '<@(repack_locales_cmd)',
            '-p', '<(OS)',
            '-g', '<(grit_out_dir)',
            '-s', '<(SHARED_INTERMEDIATE_DIR)',
            '-x', '<(repack_output_dir)/.',
            '<@(repack_extra_flags)',
            '<@(locales)',
          ],
        },
      ],
    },
  ],
}
