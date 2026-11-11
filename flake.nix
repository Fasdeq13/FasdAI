{
  description = "FasdAI build environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
            android_sdk.accept_license = true;
          };
        };

        androidComposition = pkgs.androidenv.composeAndroidPackages {
          platformVersions = [ "35" ];
          buildToolsVersions = [ "35.0.0" ];
          includeNDK = false;
          includeEmulator = false;
          includeSystemImages = false;
          cmdLineToolsVersion = "13.0";
        };
      in
      {
        devShells.default = pkgs.mkShell {
          buildInputs = [
            androidComposition.androidsdk
            pkgs.jdk17
            pkgs.gradle
            pkgs.unzip
            pkgs.zip
          ];

          ANDROID_HOME = "${androidComposition.androidsdk}/libexec/android-sdk";
          ANDROID_SDK_ROOT = "${androidComposition.androidsdk}/libexec/android-sdk";
          JAVA_HOME = "${pkgs.jdk17}";
          GRADLE_OPTS = "-Dorg.gradle.project.android.aapt2FromMavenOverride=${androidComposition.androidsdk}/libexec/android-sdk/build-tools/35.0.0/aapt2";

          shellHook = ''
            export PATH="$ANDROID_HOME/platform-tools:$PATH"
            echo "FasdAI dev shell: ANDROID_HOME=$ANDROID_HOME"
          '';
        };
      }
    );
}
