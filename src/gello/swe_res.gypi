{
  'targets' : [
    {
      'target_name': 'swe_res',
      'type': 'none',
       'dependencies': [
        'swe_android_browser_apk',
      ],
      'variables': {
        'conditions': [
          ['target_arch=="arm64"', {
            'arm_dir': '../../libs/arm64-v8a',
          }, {
            'arm_dir': '../../libs/armeabi-v7a',
          }],
        ],
      },
      'copies' : [
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/jar/',
          'files': [
            '<(PRODUCT_DIR)/lib.java/swe_engine.jar'
          ],
        },
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/assets',
          'files': [
            '<@(chrome_android_pak_input_resources)',
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
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/assets/wml',
          'files': [
            '<(PRODUCT_DIR)/swe_android_browser_apk/assets/wml/swe_wml.xsl',
            '<(PRODUCT_DIR)/swe_android_browser_apk/assets/wml/swe_wml.js',
            '<(PRODUCT_DIR)/swe_android_browser_apk/assets/wml/swe_wml.css',
          ],
        },

        #ui res
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/ui_res/',
          'files': [
            '<(PRODUCT_DIR)/res.java/ui_java.zip',
            '<(PRODUCT_DIR)/res.java/ui_strings_grd.zip',
          ],
        },
        #content res
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/content_res/',
          'files': [
            '<(PRODUCT_DIR)/res.java/content_java.zip',
            '<(PRODUCT_DIR)/res.java/content_strings_grd.zip',
          ],
        },
        #chrome_res.
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/chrome_res/',
          'files': [
            '<(PRODUCT_DIR)/res.java/chrome_java.zip',
            '<(PRODUCT_DIR)/res.java/chrome_strings_grd.zip',
          ],
        },
        #android_support_v7_res.
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/android_support_v7_res/',
          'files': [
            '<(PRODUCT_DIR)/res.java/android_support_v7_appcompat_javalib.zip',
          ],
        },
        #android_data_chart_res
        {
          'destination': '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/android_data_chart_res/',
          'files': [
            '<(PRODUCT_DIR)/res.java/android_data_chart_java.zip',
          ],
        },
        {
          'destination': '<(DEPTH)/swe/browser/generated_src/src/org/chromium/base/library_loader',
          'files': [
            '<(PRODUCT_DIR)/swe_android_browser_apk/native_libraries_java/NativeLibraries.java',
          ],
        }
      ],
      'actions': [
        {
          'action_name': 'create_lib_projects',
          'inputs': [ '<(DEPTH)/swe/tools/createAppRes.py',
                      '<(PRODUCT_DIR)/apks/SWE_AndroidBrowser.apk',
                    ],
          'outputs': ['<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/content_res/project.properties',
                      '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/content_res/AndroidManifest.xml',
                      '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/ui_res/project.properties',
                      '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/ui_res/AndroidManifest.xml',
                      '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/swe_res/project.properties',
                      '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/swe_res/AndroidManifest.xml',
                     ],
          'action': ['python', '<(DEPTH)/swe/tools/createAppRes.py',
                     '<(DEPTH)/swe/tools/createAppResources.sh',
                     '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/'],
        },
        {
           'action_name': 'merge_ui_res',
           'inputs': ['<(DEPTH)/swe/tools/merge_resources.py',
                      '<(PRODUCT_DIR)/apks/SWE_AndroidBrowser.apk',
                     ],
           'outputs': ['<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/ui_res/res/values/strings.xml'],
           'action': ['python', '<(DEPTH)/swe/tools/merge_resources.py',
                       '<(PRODUCT_DIR)/res.java/ui_java.zip',
                       '<(PRODUCT_DIR)/res.java/ui_strings_grd.zip',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/ui_res/res/',
                     ],
           'message': 'Merging UI Resources'
        },
        {
           'action_name': 'merge_content_res',
           'inputs': ['<(DEPTH)/swe/tools/merge_resources.py',
                      '<(PRODUCT_DIR)/apks/SWE_AndroidBrowser.apk',
                     ],
           'outputs': ['<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/content_res/res/values/strings.xml'],
           'action': ['python', '<(DEPTH)/swe/tools/merge_resources.py',
                       '<(PRODUCT_DIR)/res.java/content_java.zip',
                       '<(PRODUCT_DIR)/res.java/content_strings_grd.zip',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/content_res/res/',
                     ],
           'message': 'Merging Content Resources'
        },
        {
           'action_name': 'merge_swe_res',
           'inputs': ['<(DEPTH)/swe/tools/merge_resources.py',
                      '<(PRODUCT_DIR)/apks/SWE_AndroidBrowser.apk',
                     ],
           'outputs': ['<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/swe_res/res/values/strings.xml'],
           'action': ['python', '<(DEPTH)/swe/tools/merge_resources.py',
                       '<(PRODUCT_DIR)/res.java/swe_chrome_engine_java.zip',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/swe_res/res/',
                     ],
           'message': 'Merging SWE Resources'
        },
        {
           'action_name': 'merge_chrome_res',
           'inputs': ['<(DEPTH)/swe/tools/merge_resources.py',
                      '<(PRODUCT_DIR)/apks/SWE_AndroidBrowser.apk',
                     ],
           'outputs': ['<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/chrome_res/res/values/strings.xml'],
           'action': ['python', '<(DEPTH)/swe/tools/merge_resources.py',
                       '<(PRODUCT_DIR)/res.java/chrome_java.zip',
                       '<(PRODUCT_DIR)/res.java/chrome_strings_grd.zip',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/chrome_res/res/',
                     ],
           'message': 'Merging SWE Resources'
        },
        {
           'action_name': 'merge_android_support_v7_res',
           'inputs': ['<(DEPTH)/swe/tools/merge_resources.py',
                      '<(PRODUCT_DIR)/apks/SWE_AndroidBrowser.apk',
                     ],
           'outputs': ['<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/android_support_res/res/values/strings.xml'],
           'action': ['python', '<(DEPTH)/swe/tools/merge_resources.py',
                       '<(PRODUCT_DIR)/res.java/android_support_v7_appcompat_javalib.zip',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/android_support_res/res/',
                     ],
           'message': 'Merging SWE Resources'
        },
        {
           'action_name': 'merge_android_data_usage',
           'inputs': ['<(DEPTH)/swe/tools/merge_resources.py',
                      '<(PRODUCT_DIR)/apks/SWE_AndroidBrowser.apk',
                     ],
           'outputs': ['<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/android_data_chart_res/res/values/strings.xml'],
           'action': ['python', '<(DEPTH)/swe/tools/merge_resources.py',
                       '<(PRODUCT_DIR)/res.java/android_data_chart_java.zip',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/android_data_chart_res/res/',
                     ],
           'message': 'Merging SWE Resources'
        },
        {
           'action_name': 'merge_swe_libs',
           'inputs': ['<(DEPTH)/swe/tools/merge_resources.py',
                      '<(PRODUCT_DIR)/apks/SWE_AndroidBrowser.apk',
                     ],
           'outputs': ['<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libicui18n.cr.so',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libicuuc.cr.so',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libstlport_shared.so',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libsweskia.so',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libsweadrenoext_22_plugin.so',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libsweadrenoext_plugin.so',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libsta.so',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libchromium_client.so',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libswe.so',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/libsweadrenoext_plugin.so',
                      ],
           'action': ['python', '<(DEPTH)/swe/tools/copy.py',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/libs/<(arm_dir)',
                       '<(PRODUCT_DIR)/swe_android_browser_apk/swe_res/lib/',
                     ],
           'message': 'Merging SWE Libraries'
        },
      ],
    }
  ]
}
