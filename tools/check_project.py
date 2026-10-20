"""Static structure checks only; Java/Android execution is handled by Gradle."""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[1]
a = '{http://schemas.android.com/apk/res/android}'
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
assert 'package' not in manifest.attrib, 'AGP namespace belongs in Gradle'
assert not manifest.findall('uses-permission'), 'Unexpected permissions'
activity = manifest.find('application/activity')
assert activity.get(a + 'name') in ('.MainActivity', 'com.comma.calculator.MainActivity')
assert activity.get(a + 'exported') == 'true'
for path in (root / 'app/src/main/res').rglob('*.xml'):
    ET.parse(path)
for name in ('MainActivity', 'CalculatorEngine'):
    code = (root / f'app/src/main/java/com/comma/calculator/{name}.java').read_text()
    assert 'package com.comma.calculator;' in code
    assert f'class {name}' in code
    assert 'Typeface.MEDIUM' not in code
build = (root / 'app/build.gradle').read_text()
for token in ("namespace 'com.comma.calculator'", "applicationId 'com.comma.calculator'", 'compileSdk 35', 'minSdk 21', 'targetSdk 35', 'versionCode 2', "versionName '1.1.0'", 'junit:junit:4.13.2'):
    assert token in build, token
workflow = json.loads((root / '.github/workflows/build-apk.yml').read_text())
assert workflow['on']['workflow_dispatch']['inputs']['allow_new_signature']['default'] is False
steps = workflow['jobs']['build']['steps']
assert any(':app:testReleaseUnitTest :app:assembleRelease' in s.get('run', '') for s in steps)
assert (root / 'app/src/test/java/EngineTest.java').is_file()
assert (root / 'app/src/test/java/EngineRegressionTest.java').is_file()
assert not list(root.rglob('*.jks')), 'Private key must not be in the project'
print('Static project checks passed. This does not execute Java tests or Android UI.')
