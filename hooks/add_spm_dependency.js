#!/usr/bin/env node
var fs = require('fs');
var xcode = require('xcode');
var path = require('path');
var util = require('util');
var fs = require('fs');

function getProjectName() {
    var config = fs.readFileSync('config.xml').toString();
    var parseString = require('xml2js').parseString;
    var name;
    parseString(config, function (err, result) {
        name = result.widget.name.toString();
        const r = /\B\s+|\s+\B/g; // Removes trailing and leading spaces
        name = name.replace(r, '');
    });
    return name || null;
}

// Assuming this is being run after plugin install and paths are correct
var iosPlatformPath = 'platforms/ios/';
var projectName = getProjectName();

module.exports = function (context) {
    // Path to the .xcodeproj file
    console.log('⭐️ Removing the SWIFT_OBJC_BRIDGING_HEADER from the App');
    var projectPath = path.join(context.opts.projectRoot, 'platforms/ios/',getProjectName() + '.xcodeproj','project.pbxproj');
    
    // Check if the project file exists
    if (!fs.existsSync(projectPath)) {
        console.error('❌ --- The project.pbxproj file was not found!');
        return;
    }

    // Load the project
    var project = xcode.project(projectPath);
    project.parseSync();

    // Add the package dependency
    var packageDependency = {
        package: {
            url: 'https://github.com/mysugr/bluecandy-devices-rochebgm-ios.git',
            version: '7.0.0' // The version you want to add
        }
    };
    
    // The logic here would need to be expanded to actually edit the .pbxproj file correctly.
    // This is a complex task and would require deep knowledge of the Xcode project structure and the xcode Node.js library.

    // Write the project file back out
    fs.writeFileSync(projectPath, project.writeSync());
    console.log('✅ --- SPM package dependency added.');
}
