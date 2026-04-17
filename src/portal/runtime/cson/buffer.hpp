#pragma once

#include <gc/gc.h>

#include <string>
#include <nlohmann/json.hpp>

namespace portal::runtime::cson::buffer
{
  enum class type : int
  {
    null,
    boolean,
    integer,
    floating,
    string,
    unknown,
  };

  struct reader
  {
    nlohmann::json buffer;
    nlohmann::json::iterator it;
    nlohmann::json::iterator end;

    reader(nlohmann::json &&j) : buffer(std::move(j))
    {
      it = buffer.begin();
      end = buffer.end();
    }

    void next_null()
    {
      if (it == end)
      {
        throw std::out_of_range("Iterator reached the end of the JSON buffer");
      }

      std::advance(it, 1);
    }

    bool next_bool()
    {
      if (it == end)
      {
        throw std::out_of_range("Iterator reached the end of the JSON buffer");
      }

      auto value(it->get<bool>());
      std::advance(it, 1);
      return value;
    }

    long next_long()
    {
      if (it == end)
      {
        throw std::out_of_range("Iterator reached the end of the JSON buffer");
      }

      auto value(it->get<long>());
      std::advance(it, 1);
      return value;
    }

    double next_double()
    {
      if (it == end)
      {
        throw std::out_of_range("Iterator reached the end of the JSON buffer");
      }

      auto value(it->get<double>());
      std::advance(it, 1);
      return value;
    }

    std::string next_string()
    {
      if (it == end)
      {
        throw std::out_of_range("Iterator reached the end of the JSON buffer");
      }

      auto value(it->get<std::string>());
      std::advance(it, 1);
      return value;
    }

    type next_type()
    {
      if (it == end)
      {
        return type::null;
      }

      switch (it->type())
      {
      case nlohmann::json::value_t::null:
        return type::null;
      case nlohmann::json::value_t::string:
        return type::string;
      case nlohmann::json::value_t::number_integer:
      case nlohmann::json::value_t::number_unsigned:
        return type::integer;
      case nlohmann::json::value_t::number_float:
        return type::floating;
      case nlohmann::json::value_t::boolean:
        return type::boolean;
      default:
        return type::unknown;
      }
    }
  };

  reader *create_reader(std::string json_str)
  {
    auto json(nlohmann::json::parse(json_str));
    return new (UseGC) reader{std::move(json)};
  }

  struct writer
  {
    nlohmann::json buffer;

    writer() : buffer(nlohmann::json::array())
    {
    }

    void push_null()
    {
      buffer.push_back(nullptr);
    }

    void push_long(long value)
    {
      buffer.push_back(value);
    }

    void push_bool(bool value)
    {
      buffer.push_back(value);
    }

    void push_double(double value)
    {
      buffer.push_back(value);
    }

    void push_string(const std::string &value)
    {
      buffer.push_back(value);
    }

    std::string str() const
    {
      return buffer.dump();
    }
  };

  writer *create_writer()
  {
    return new (UseGC) writer{};
  }
}