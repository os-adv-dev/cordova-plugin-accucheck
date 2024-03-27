#!/usr/bin/env node
const fs = require('fs');
const path = require('path');
const xml2js = require('xml2js');

const args = process.argv
var userName;
var password;

    for (const arg of args) {  
      if (arg.includes('ANDROID_MYSUGR_SDK_USERNAME')){
        var stringArray = arg.split("=");
        userName = stringArray.slice(-1).pop();
        console.log("Value from ANDROID_MYSUGR_SDK_USERNAME: "+userName);
      }
      if (arg.includes('ANDROID_MYSUGR_SDK_PASSWORD')){
        var stringArray = arg.split("=");
        password = stringArray.slice(-1).pop();
        console.log("Value from ANDROID_MYSUGR_SDK_PASSWORD: "+password);
      }
    }

const variables = {
    ANDROID_MYSUGR_SDK_USERNAME: userName || 'USER_NAME_PLACEHOLDER',
    ANDROID_MYSUGR_SDK_PASSWORD: password || 'PASSWORD_PLACEHOLDER'
};


module.exports = function (context) {
    // Caminho até o build.gradle que precisa ser alterado
    const gradlePath = path.join(context.opts.projectRoot, 'platforms', 'android', 'com.outsystems.experts.accucheck', 'AccuCheckSampleApp-build.gradle');
    const configXmlPath = path.join(context.opts.projectRoot, 'config.xml');
    
    // Lê o config.xml para buscar as preferências
    fs.readFile(configXmlPath, 'utf-8', function (err, data) {
        if (err) throw new Error('Unable to find config.xml: ' + err);

        xml2js.parseString(data, function (err, result) {
            if (err) throw new Error('Unable to parse config.xml: ' + err);

            const preferences = result.widget.platform[0].preference;
            let sdkUsername = '';
            let sdkPassword = '';

            // Busca as preferências de username e password
            preferences.forEach(function(pref) {
                if (pref.$.name === 'ANDROID_MYSUGR_SDK_USERNAME') {
                    sdkUsername = pref.$.default || '';
                }
                if (pref.$.name === 'ANDROID_MYSUGR_SDK_PASSWORD') {
                    sdkPassword = pref.$.default || '';
                }
            });

            sdkUsername = variables.ANDROID_MYSUGR_SDK_USERNAME;
            sdkPassword = variables.ANDROID_MYSUGR_SDK_PASSWORD;

            if (!sdkUsername || !sdkPassword) {
                throw new Error('SDK username and/or password preferences not found in config.xml');
            }

            // Lê o arquivo gradle e substitui os placeholders
            let gradleData = fs.readFileSync(gradlePath, 'utf-8');
            gradleData = gradleData.replace('USER_NAME_PLACEHOLDER', sdkUsername)
                                   .replace('PASSWORD_PLACEHOLDER', sdkPassword);

            // Escreve de volta as alterações no arquivo gradle
            fs.writeFileSync(gradlePath, gradleData, 'utf-8');
            console.log('Updated AccuCheckSampleApp-build.gradle with actual SDK credentials.');
        });
    });
};