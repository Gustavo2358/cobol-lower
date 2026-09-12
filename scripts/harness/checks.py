"""Offline documentary and certification predicates. Never infer execution from prose."""
import hashlib
import json
from collections import Counter, defaultdict
from pathlib import Path, PurePosixPath
import re
from urllib.parse import unquote, urlsplit

import yaml


def digest(data):
    return hashlib.sha256(data).hexdigest()


def relative(path):
    return (isinstance(path, str) and bool(path) and not PurePosixPath(path).is_absolute()
            and ".." not in PurePosixPath(path).parts and "\\" not in path)


def unique_pairs(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError("duplicate key: " + str(key))
        result[key] = value
    return result


class StrictYaml(yaml.SafeLoader):
    pass


def yaml_mapping(loader, node):
    return unique_pairs([(loader.construct_object(k), loader.construct_object(v)) for k, v in node.value])


StrictYaml.add_constructor(yaml.resolver.BaseResolver.DEFAULT_MAPPING_TAG, yaml_mapping)


def read_data(path):
    text = path.read_text(encoding="utf-8")
    if path.suffix == ".json":
        return json.loads(text, object_pairs_hook=unique_pairs)
    return yaml.load(text, Loader=StrictYaml)
