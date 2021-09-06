#import "PluginCodelabPlugin.h"
#if __has_include(<plugin_codelab/plugin_codelab-Swift.h>)
#import <plugin_codelab/plugin_codelab-Swift.h>
#else
// Support project import fallback if the generated compatibility header
// is not copied when this plugin is created as a library.
// https://forums.swift.org/t/swift-static-libraries-dont-copy-generated-objective-c-header/19816
#import "plugin_codelab-Swift.h"
#endif

@implementation PluginCodelabPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftPluginCodelabPlugin registerWithRegistrar:registrar];
}
@end
