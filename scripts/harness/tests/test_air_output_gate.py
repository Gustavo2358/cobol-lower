"""AIR output must execute in semantic/full, including when Maven succeeds with no suite."""
from pathlib import Path
import sys
import unittest
import tempfile
import shutil
import zipfile
from unittest.mock import patch
ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0,str(ROOT/'scripts/harness'))
import run as harness
from architecture import source_errors, dependency_errors, output_boundary_errors

class AirOutputGate(unittest.TestCase):
    def test_missing_zero_or_duplicate_suite_rejected(self):
        for marker in ('', 'LOWER_AIR_OUTPUT_TESTS=0\n', 'LOWER_AIR_OUTPUT_TESTS=1\nLOWER_AIR_OUTPUT_TESTS=1\n'):
            with patch.object(harness,'verify_dependency'), patch.object(harness,'maven',return_value=[]), patch.object(harness,'run',return_value='LOWER_TESTS=1\nLOWER_TESTS=1\n'+marker):
                with self.assertRaisesRegex(RuntimeError,'AIR output tests absent/zero/duplicate'):
                    harness.semantic()
    def test_output_suite_present(self):
        with patch.object(harness,'verify_dependency'), patch.object(harness,'maven',return_value=[]), patch.object(harness,'run',return_value='LOWER_TESTS=1\nLOWER_TESTS=1\nLOWER_AIR_OUTPUT_TESTS=1\nLOWER_PRODUCTION_TESTS=1\nPRODUCTION_SUCCESS_PROBE data=400 moves=400\n'):
            harness.semantic()
    def test_production_probe_is_mandatory(self):
        output = 'LOWER_TESTS=1\nLOWER_TESTS=1\nLOWER_AIR_OUTPUT_TESTS=1\n'
        with patch.object(harness,'verify_dependency'), patch.object(harness,'maven',return_value=[]), patch.object(harness,'run',return_value=output):
            with self.assertRaisesRegex(RuntimeError,'production path probe absent/zero'):
                harness.semantic()
    def test_production_limit_order_is_mandatory_in_performance(self):
        output = 'LOWER_TESTS=1\nLOWER_TESTS=1\nLOWER_AIR_OUTPUT_TESTS=1\nLOWER_PRODUCTION_TESTS=1\nPRODUCTION_SUCCESS_PROBE data=400 moves=400\n'
        with patch.object(harness,'verify_dependency'), patch.object(harness,'maven',return_value=[]), patch.object(harness,'run',return_value=output):
            with self.assertRaisesRegex(RuntimeError,'production limit-order probe absent'):
                harness.semantic(('-Dlower.performance=true',))
    def test_core_codec_source_is_forbidden(self):
        self.assertTrue(source_errors('import io.github.gustavo2358.air.json.AirJson;'))
    def test_core_codec_maven_is_forbidden(self):
        tree=dict(groupId='io.github.gustavo2358',artifactId='cobol-lower-core',version='0.1.0-SNAPSHOT',children=[dict(groupId='io.github.gustavo2358',artifactId=a,version='0.1.0-SNAPSHOT') for a in ('air-java','air-json')])
        self.assertTrue(dependency_errors(tree))

class OutputOwnership(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory(prefix='lower-output-boundary-')
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)
        for module in ('core', 'adapters'):
            directory = self.root / module
            (directory / 'target').mkdir(parents=True)
            shutil.copy2(ROOT / module / 'pom.xml', directory / 'pom.xml')
            with zipfile.ZipFile(directory / 'target' / ('cobol-lower-' + module + '-0.1.0-SNAPSHOT.jar'), 'w'):
                pass
        shutil.copy2(ROOT / 'pom.xml', self.root / 'pom.xml')
        self.assertEqual([], output_boundary_errors(self.root))

    def test_shared_codec_cannot_be_test_only(self):
        p = self.root / 'adapters/pom.xml'
        p.write_text(p.read_text().replace('<artifactId>air-json</artifactId>', '<artifactId>air-json</artifactId><scope>test</scope>'))
        self.assertTrue(any('compile dependency' in e for e in output_boundary_errors(self.root)))

    def test_copied_codec_ownership(self):
        p = self.root / 'adapters/src/main/java/copied/BindingWriter.java'
        p.parent.mkdir(parents=True)
        p.write_text('package copied; class BindingWriter {}')
        self.assertTrue(any('copied codec' in e for e in output_boundary_errors(self.root)))

    def test_shaded_and_bundled_codec(self):
        p = self.root / 'adapters/pom.xml'
        p.write_text(p.read_text().replace('</project>', '<build><plugins><plugin><artifactId>maven-shade-plugin</artifactId></plugin></plugins></build></project>'))
        self.assertTrue(any('shaded/copied' in e for e in output_boundary_errors(self.root)))
        with zipfile.ZipFile(self.root / 'core/target/cobol-lower-core-0.1.0-SNAPSHOT.jar', 'a') as archive:
            archive.writestr('io/github/gustavo2358/air/json/AirJson.class', b'forbidden owner')
        self.assertTrue(any('bundled in product' in e for e in output_boundary_errors(self.root)))
