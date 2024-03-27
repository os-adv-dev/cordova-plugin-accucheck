var fs = require('fs');
var path = require('path');
const xcode = require('xcode');

function getProjectName() {
    var config = fs.readFileSync('config.xml').toString();
    var parseString = require('xml2js').parseString;
    var name;
    parseString(config, function (err, result) {
        name = result.widget.name[0].toString().trim();
    });
    return name;
}

var libsSPM = `
/* Begin XCRemoteSwiftPackageReference section */
8D71ABDB2BAB41B5003C53EA /* XCRemoteSwiftPackageReference "dataparsing-ios" */ = {
    isa = XCRemoteSwiftPackageReference;
    repositoryURL = "https://pcamilojunior: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@github.com/mysugr/dataparsing-ios.git";
    requirement = {
        kind = upToNextMajorVersion;
        minimumVersion = 4.0.0;
    };
};
/* End XCRemoteSwiftPackageReference section */

/* Begin XCSwiftPackageProductDependency section */
		8D71ABDC2BAB41B5003C53EA /* DataParsing */ = {
			isa = XCSwiftPackageProductDependency;
			package = 8D71ABDB2BAB41B5003C53EA /* XCRemoteSwiftPackageReference "dataparsing-ios" */;
			productName = DataParsing;
		};
/* End XCSwiftPackageProductDependency section */
	};
`;

var libPBXBuildFileMySugr = `
		8D71ABDD2BAB41B5003C53EA /* DataParsing in Frameworks */ = {isa = PBXBuildFile; productRef = 8D71ABDC2BAB41B5003C53EA /* DataParsing */; };
`;

var text = `8D71ABDD2BAB41B5003C53EA /* DataParsing in Frameworks */,`;

module.exports = function (context) {
    console.log('⭐️ Integrating Swift Package Manager dependencies into the project');
    var pbxPath = path.join(context.opts.projectRoot, 'platforms/ios/', getProjectName() + '.xcodeproj', 'project.pbxproj');

    let project = xcode.project(pbxPath);
    project.parseSync();

    // Add libPBXBuildFileMySugr content before the end of PBXBuildFile section
    let pbxBuildFileSectionEnd = project.pbxBuildFileSection() && Object.keys(project.pbxBuildFileSection()).pop();
    project.addToBuildFiles(libPBXBuildFileMySugr, pbxBuildFileSectionEnd, 'Frameworks');

    // Add text content in PBXFrameworksBuildPhase section
    var frameworksBuildPhase = project.pbxFrameworksBuildPhaseObj(project.getFirstTarget().uuid);
    frameworksBuildPhase.files.push(text);

    // Add DataParsing to packageProductDependencies in PBXNativeTarget section
    var nativeTargetSection = project.pbxNativeTargetSection();
    for (var target in nativeTargetSection) {
        if (nativeTargetSection[target].isa === 'PBXNativeTarget') {
            nativeTargetSection[target].packageProductDependencies = nativeTargetSection[target].packageProductDependencies || [];
            nativeTargetSection[target].packageProductDependencies.push('8D71ABDC2BAB41B5003C53EA /* DataParsing */,');
            break; // Remove this line if you want to add to all targets
        }
    }

    // Add XCRemoteSwiftPackageReference "dataparsing-ios" to PBXProject section
    var projectSection = project.pbxProjectSection();
    for (var key in projectSection) {
        if (projectSection[key].isa === 'PBXProject') {
            projectSection[key].packageReferences = projectSection[key].packageReferences || [];
            projectSection[key].packageReferences.push('8D71ABDB2BAB41B5003C53EA /* XCRemoteSwiftPackageReference "dataparsing-ios" */,');
            break; // Remove this line if you want to add to all projects
        }
    }

    // Write the modified content back to the pbxproj file
    fs.writeFileSync(pbxPath, project.writeSync());
    console.log('✅ Swift Package Manager dependencies have been successfully integrated.');
};