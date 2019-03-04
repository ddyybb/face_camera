//
//  FDCam.swift
//  Runner
//
//  Created by Dmitriy Baranov on 01/03/2019.
//  Copyright © 2019 Dmitriy Baranov. All rights reserved.
//

import AVFoundation

class FDCam {
    let texture = FDCamTexture()
    let previewSize: CGSize
    
    private let captureSession: AVCaptureSession
    private let captureDevice: AVCaptureDevice
    private let captureVideoInput: AVCaptureInput
    private let captureVideoOutput: AVCaptureVideoDataOutput
    private let sessionQueue = DispatchQueue(label: "FDCamCaptureSessionQueue")
    
    init?(
        //cameraName: String?,
        //resolutionPreset: String?
    ) throws {
        // getting camera name
        let discoverySession = AVCaptureDevice.DiscoverySession(
            deviceTypes: [
                AVCaptureDevice.DeviceType.builtInWideAngleCamera
            ],
            mediaType: AVMediaType.video,
            position: AVCaptureDevice.Position.front
        )
        
        guard
            let cameraName = discoverySession.devices.first?.uniqueID
        else {
            print("Camera name cannot be nil")
            return nil
        }
        
        captureSession = AVCaptureSession()
        captureSession.beginConfiguration()

//        let resolutionPreset = "high"
//
//        switch resolutionPreset {
//        case "high":
//            captureSession.sessionPreset = .hd1280x720
//            previewSize = CGSize(width: 1280, height: 720)
//        case "medium":
//            captureSession.sessionPreset = .vga640x480
//            previewSize = CGSize(width: 640, height: 480)
//        default:
//            captureSession.sessionPreset = .cif352x288
//            previewSize = CGSize(width: 352, height: 288)
//        }

        captureSession.sessionPreset = .high
        //previewSize = CGSize(width: 640, height: 360)
        previewSize = CGSize(width: 1280, height: 720)
        
        // Configure Input
        
        guard let device = AVCaptureDevice(uniqueID: cameraName) else {
            print("Failed to init a capture device.")
            return nil
        }
        
        captureDevice = device
        captureVideoInput = try AVCaptureDeviceInput(device: device)
        
        guard captureSession.canAddInput(captureVideoInput) else {
            print("Failed to add capture session input.")
            return nil
        }
        
        captureSession.addInputWithNoConnections(captureVideoInput)
        
        // Configure Output
        
        captureVideoOutput = AVCaptureVideoDataOutput()
        captureVideoOutput.videoSettings = [
            (kCVPixelBufferPixelFormatTypeKey as String): kCVPixelFormatType_32BGRA
        ]
        
        //captureVideoOutput.alwaysDiscardsLateVideoFrames = true
        
        captureVideoOutput.setSampleBufferDelegate(
            texture,
            queue: DispatchQueue(label: "FDCamCaptureVideoOutputQueue")
        )
        
        guard captureSession.canAddOutput(captureVideoOutput) else {
            print("Failed to add capture session output.")
            return nil
        }
        
        captureSession.addOutputWithNoConnections(captureVideoOutput)
        
        // Configure connection
        
        let connection = AVCaptureConnection(
            inputPorts: captureVideoInput.ports,
            output: captureVideoOutput
        )
        
        if device.position == .front {
            connection.isVideoMirrored = true
        }
        
        
        guard connection.isVideoOrientationSupported else {
            print("Failed to change video orientation")
            return nil
        }
        
        connection.videoOrientation = .portrait
        
        guard captureSession.canAdd(connection) else {
            print("Failed to add capture session connection")
            return nil
        }
        
        captureSession.add(connection)
        
        captureSession.commitConfiguration()
    }
    
    func start() {
        sessionQueue.async {
            self.captureSession.startRunning()
        }
    }
    
    func stop() {
        sessionQueue.async {
            self.captureSession.stopRunning()
        }
    }

    func savePreview() {
        if let image = texture.getImage() {
            let img = UIImage(cgImage: image)
            UIImageWriteToSavedPhotosAlbum(img, nil, nil, nil)
        }
    }
}
