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

module.exports = function (context) {
    console.log('⭐️ Integrating Swift Package Manager dependencies into the project');
    var pbxPath = path.join(context.opts.projectRoot, 'platforms/ios/', getProjectName() + '.xcodeproj', 'project.pbxproj');

    let project = xcode.project(pbxPath);
    project.parseSync();

    let pbxContent = fs.readFileSync(pbxPath, 'utf8');
    let insertionPoint = pbxContent.lastIndexOf('/* End XCConfigurationList section */');

    // If the insertion point is found, proceed to modify the content
    if (insertionPoint !== -1) {
        let modifiedContent = pbxContent.substring(0, insertionPoint) + libsSPM + '\n' + pbxContent.substring(insertionPoint);

        // Write the modified content back to the pbxproj file
        fs.writeFileSync(pbxPath, modifiedContent, 'utf8');
        console.log('✅ Swift Package Manager dependencies have been successfully integrated.');
    } else {
        console.log('❌ Could not find a suitable insertion point for Swift Package Manager dependencies.');
    }

    // Find the end of the PBXBuildFile section
    let pbxBuildFileSectionEnd = pbxContent.indexOf('/* End PBXBuildFile section */');

    // If the end of the PBXBuildFile section is found, insert the content just before it
    if (pbxBuildFileSectionEnd !== -1) {
        let beforeSection = pbxContent.substring(0, pbxBuildFileSectionEnd);
        let afterSection = pbxContent.substring(pbxBuildFileSectionEnd);

        // Insert libPBXBuildFileMySugr content and reconstruct the pbxContent
        pbxContent = beforeSection + libPBXBuildFileMySugr + '\n' + afterSection;

        // Write the modified content back to the pbxproj file
        fs.writeFileSync(pbxPath, pbxContent, 'utf8');
        console.log('✅ Custom SPM dependency has been added to the PBXBuildFile section.');
    } else {
        console.log('❌ Could not find the PBXBuildFile section in the project.pbxproj file.');
    }
};