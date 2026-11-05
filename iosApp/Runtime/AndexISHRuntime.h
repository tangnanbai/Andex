#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

typedef void (^AndexISHProgressBlock)(NSString *phase, NSString *detail, double fraction);
typedef void (^AndexISHFileWriteProgressBlock)(NSUInteger bytesCopied);
typedef void (^AndexISHCompletionBlock)(NSError * _Nullable error);
typedef void (^AndexISHOutputBlock)(NSData *bytes);
typedef void (^AndexISHExitBlock)(int exitCode, int signal);

@interface AndexISHRuntime : NSObject

+ (instancetype)sharedRuntime;

@property(nonatomic, readonly, getter=isInitialized) BOOL initialized;

- (void)initializeWithProgress:(AndexISHProgressBlock)progress
                    completion:(AndexISHCompletionBlock)completion;

- (int)startExecutable:(NSString *)executable
             arguments:(NSArray<NSString *> *)arguments
           environment:(NSDictionary<NSString *, NSString *> *)environment
      workingDirectory:(NSString *)workingDirectory
        pseudoTerminal:(BOOL)pseudoTerminal
    remoteDebuggingPipe:(BOOL)remoteDebuggingPipe
        standardOutput:(AndexISHOutputBlock)stdoutBlock
         standardError:(AndexISHOutputBlock)stderrBlock
                   exit:(AndexISHExitBlock)exitBlock;

- (BOOL)writeStdin:(NSData *)bytes processId:(int)processId;
- (void)closeStdinForProcessId:(int)processId;
- (void)signalProcessId:(int)processId signal:(int)signal;
- (void)resizeTerminalForProcessId:(int)processId columns:(int)columns rows:(int)rows;

- (BOOL)fileExists:(NSString *)path;
- (nullable NSData *)readFile:(NSString *)path error:(NSError **)error;
- (nullable NSData *)readFile:(NSString *)path
                 maximumBytes:(NSUInteger)maximumBytes
                        error:(NSError **)error;
- (nullable NSData *)readFilePrefix:(NSString *)path
                       maximumBytes:(NSUInteger)maximumBytes
                              error:(NSError **)error;
- (BOOL)exportFile:(NSString *)path toURL:(NSURL *)destinationURL error:(NSError **)error;
- (BOOL)writeFile:(NSString *)path data:(NSData *)data executable:(BOOL)executable error:(NSError **)error;
- (BOOL)writeFile:(NSString *)path
             data:(NSData *)data
       executable:(BOOL)executable
         progress:(nullable AndexISHFileWriteProgressBlock)progress
            error:(NSError **)error;
- (BOOL)createDirectories:(NSString *)path error:(NSError **)error;
- (nullable NSArray<NSDictionary<NSString *, id> *> *)listDirectory:(NSString *)path
                                                               error:(NSError **)error;
- (BOOL)movePath:(NSString *)sourcePath toPath:(NSString *)destinationPath error:(NSError **)error;
- (BOOL)removePath:(NSString *)path recursive:(BOOL)recursive error:(NSError **)error;
- (BOOL)bindHostPath:(NSString *)hostPath guestPath:(NSString *)guestPath readOnly:(BOOL)readOnly error:(NSError **)error;

@end

NS_ASSUME_NONNULL_END
