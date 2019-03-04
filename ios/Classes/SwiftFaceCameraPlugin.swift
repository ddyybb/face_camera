import Flutter
import UIKit

public class SwiftFaceCameraPlugin: NSObject, FlutterPlugin {
    
    private var camera: FDCam?
    private let registry: FlutterTextureRegistry
    
    init(registry: FlutterTextureRegistry) {
        self.registry = registry
    }
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(
            name: "flutter.io/SurfaceTest",
            binaryMessenger: registrar.messenger()
        )
        
        let instance = SwiftFaceCameraPlugin(registry: registrar.textures())
        registrar.addMethodCallDelegate(instance, channel: channel)
    }
    
    public func handle(
        _ call: FlutterMethodCall,
        result: @escaping FlutterResult
    ) {
        //let arguments = call.arguments as? [String: String]
        
        switch call.method {
        case "init":
            do {
                camera = try FDCam(
                    //cameraName: arguments?["cameraName"],
                    //resolutionPreset: arguments?["resolutionPreset"]
                )
                
                initializeCamera(result: result)
            } catch let error {
                result(FlutterError(
                    code: "fdcam.camera.initialize",
                    message: error.localizedDescription,
                    details: nil
                ))
            }

        case "save":
            camera?.savePreview()

        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    private func initializeCamera(
        result: FlutterResult
    ) {
        guard
            let camera = self.camera
        else {
            result(FlutterError(
                code: "fdcam.camera.initialize",
                message: "Camera not initialized",
                details: nil
            ))
            return
        }
        
        let textureId = registry.register(camera.texture)
        
        camera.texture.onFrameAvailable = { [weak self] in
            self?.registry.textureFrameAvailable(textureId)
        }
        
        let data: [String: Int64] = [
            "textureId" : textureId,
            "previewWidth" : Int64(camera.previewSize.width),
            "previewHeight" : Int64(camera.previewSize.height)
        ]
        
        camera.start()
        
        result(data)
    }
}
