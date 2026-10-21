#pragma once

#include <nlohmann/json.hpp>

#include <jtl/immutable_string.hpp>

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

    reader(jtl::immutable_string const &json_str)
        : buffer(nlohmann::json::parse(json_str)), it{buffer.begin()}, end{buffer.end()}
    {
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

      auto const value(it->get<bool>());
      std::advance(it, 1);
      return value;
    }

    long next_long()
    {
      if (it == end)
      {
        throw std::out_of_range("Iterator reached the end of the JSON buffer");
      }

      auto const value(it->get<long>());
      std::advance(it, 1);
      return value;
    }

    double next_double()
    {
      if (it == end)
      {
        throw std::out_of_range("Iterator reached the end of the JSON buffer");
      }

      auto const value(it->get<double>());
      std::advance(it, 1);
      return value;
    }

    jtl::immutable_string next_string()
    {
      if (it == end)
      {
        throw std::out_of_range("Iterator reached the end of the JSON buffer");
      }

      auto const value(it->get<jtl::immutable_string>());
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

    void push_string(jtl::immutable_string const &value)
    {
      buffer.push_back(value);
    }

    jtl::immutable_string str() const
    {
      return buffer.dump();
    }
  };
}