import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
BASE = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('receiver_profiles', BASE / 'manage.py')
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
class ProfileTests(unittest.TestCase):
    def test_profiles_keep_pin_protection_and_receiver_volume(self):
        for path in (BASE / 'profiles').glob('*.json'):
            values = m.validate(json.loads(path.read_text()))
            self.assertTrue(values['require_pin']); self.assertFalse(values['sync_client_volume'])
    def test_invalid_settings_are_rejected_before_any_device_change(self):
        for values in [{'server_port': 7000}, {'max_fps': True}, {'server_name': 'bad\nname'}, {'low_latency': 'true'}]:
            with self.assertRaises(ValueError): m.validate(values)
    def test_partial_failure_preserves_original_values_and_completed_steps(self):
        class FakeReceiver:
            def settings(self): return {'low_latency': {'value': False}, 'require_pin': {'value': False}}
            def set(self, key, value):
                if key == 'require_pin': raise RuntimeError('Simulated device disconnect')
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'undo.json'
            with self.assertRaises(RuntimeError): m.apply(FakeReceiver(), {'low_latency': True, 'require_pin': True}, path)
            saved = json.loads(path.read_text())
            self.assertEqual(['low_latency'], saved['completed'])
            self.assertEqual({'low_latency': False, 'require_pin': False}, saved['before'])
if __name__ == '__main__': unittest.main()
