import os
import tempfile

if __name__ == '__main__':
    java_tmpdir = tempfile.mkdtemp()
    os.environ["GRAALVM_OPTIONS"] = f"-Xmx1G -Djava.io.tmpdir={java_tmpdir}" # Example flag to test propagation
    import pypowsybl
    # the options reached the isolate: the computation manager's working directory is created in java.io.tmpdir
    pypowsybl.network.load(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'battery.xiidm'))
    assert any(name.startswith('itools_common_') for name in os.listdir(java_tmpdir)), os.listdir(java_tmpdir)
