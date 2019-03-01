#import "FaceCameraPlugin.h"
#import <face_camera/face_camera-Swift.h>

@implementation FaceCameraPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFaceCameraPlugin registerWithRegistrar:registrar];
}
@end
